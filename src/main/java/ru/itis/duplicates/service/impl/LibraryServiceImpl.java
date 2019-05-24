package ru.itis.duplicates.service.impl;

import lombok.extern.java.Log;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.*;
import ru.itis.duplicates.service.ArticleService;
import ru.itis.duplicates.service.LibraryService;
import ru.itis.duplicates.task.LinksFinder;
import ru.itis.duplicates.util.Utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log
public class LibraryServiceImpl implements LibraryService {
    private ArticleService articleService;
    private Dao dao;
    private Queue<LinkFinderInfo> linksFindersDispatcher;

    public LibraryServiceImpl() {
        this.articleService = new ArticleServiceImpl();
        this.dao = new DaoImpl();
        this.linksFindersDispatcher = new ConcurrentLinkedQueue<>();
        new Thread(new QueueDispatcher()).start();
    }

    public LibraryServiceImpl(ArticleService articleService, Dao dao) {
        this.articleService = articleService;
        this.dao = dao;
        this.linksFindersDispatcher = new ConcurrentLinkedQueue<>();
        new Thread(new QueueDispatcher()).start();
    }

    public class QueueDispatcher implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (linksFindersDispatcher != null && !linksFindersDispatcher.isEmpty()) {
                    LinkFinderInfo head = linksFindersDispatcher.peek();
                    if (Objects.nonNull(head)) {
                        if (head.getStatus() == LinkFinderStatus.NEW) {
                            saveLibrary(head);
                        }
                        if (head.getStatus() == LinkFinderStatus.FINISHED) {
                            linksFindersDispatcher.poll();
                        }
                    }
                }
            }
        }
    }

    //TODO: при добавлении не надо жестко смотреть по названию. 1 и тот же ресурс может быть как url так и файлами
    @Override
    public void saveLibrary(LinkFinderInfo info) {
        try {
            String library = info.getLibrary();
            String rootUrl = Utils.getRootUrl(library);

            //TODO: либо так либо так. лучше фабрика или енум
            if (!dao.isLibraryExists(rootUrl)) {
                List<String> parsedClarifications = parseClarifications(info.getClarifications());

                info.setFinder(new LinksFinder(library, rootUrl, parsedClarifications, true));

                info.setStatus(LinkFinderStatus.FINDING_LINKS);
                info.getFinder().getAllLinksFromSite();

                List<String> clarificationLinksFound = info.getFinder().getClarificationLinksFound();

                ArticleUrlPattern articleUrlPattern = null;
                if (null != clarificationLinksFound && clarificationLinksFound.size() >= 2) {
                    articleUrlPattern = Utils.getArticleUrlPattern(clarificationLinksFound);
                }

                if (null != articleUrlPattern) {
                    dao.saveLibrary(rootUrl, articleUrlPattern.getBeforeRange(), articleUrlPattern.getAfterRange());
                    info.setFinder(new LinksFinder(library, rootUrl, parsedClarifications, articleUrlPattern, 0));
                } else {
                    dao.saveLibrary(rootUrl, null, null);
                    info.setFinder(new LinksFinder(library, rootUrl, parsedClarifications, null, 0));
                }

                for (String parsedClarification : parsedClarifications) {
                    dao.saveClarificationForLibrary(new Clarification(rootUrl, parsedClarification));
                }

            } else {
                List<String> parsedLinks = dao.getArticlesFromLibrary(rootUrl);

                Library existedLibrary = dao.getLibrary(rootUrl);

                List<String> existedClarifications = dao.getClarificationsForLibrary(rootUrl);

                String beforeRange = existedLibrary.getBeforeRange();
                String afterRange = existedLibrary.getAfterRange();
                ArticleUrlPattern articleUrlPattern;
                if (null != beforeRange) {
                    articleUrlPattern = new ArticleUrlPattern(beforeRange, afterRange);
                } else {
                    articleUrlPattern = ArticleUrlPattern.getNoPatternInstance();
                }
                info.setFinder(new LinksFinder(library, rootUrl, existedClarifications, parsedLinks,
                        articleUrlPattern, existedLibrary.getLastParsedInRange()));

                System.out.println("exists: " + rootUrl);
            }

            info.setStatus(LinkFinderStatus.PROCESSING);
            info.getFinder().getAllLinksFromSite();
            info.setStatus(LinkFinderStatus.FINISHING);

            LocalDateTime currentTime = LocalDateTime.now();

            dao.updateLibrary(new Library(rootUrl, Timestamp.valueOf(currentTime), info.getFinder().getLastParsed().longValue()));

            Library libraryFromDb = dao.getLibrary(rootUrl);

            dao.recalculateWeight(rootUrl, libraryFromDb.getWordsCount());

            info.setStatus(LinkFinderStatus.FINISHED);

        } catch (Exception e) {
            System.out.println("links finder gg: " + e);
        }
        //TODO: сделать норм return
    }

    @Override
    public synchronized QueueInfo addInQueue(String library, List<String> clarifications) {
        boolean urlReachable = Utils.isUrlReachable(library, 1000);
        if (urlReachable) {
            System.out.println("URL: " + library + " is reachable");
            LinkFinderInfo newInstance = LinkFinderInfo.getNewInstance(null, library, clarifications);
            boolean isContains = linksFindersDispatcher.contains(newInstance);
            if (!isContains) {
                linksFindersDispatcher.add(newInstance);
            } else {
                System.out.println("URL: " + library + " is already in queue");
            }
        } else {
            System.out.println("URL: " + library + " is NOT reachable");
        }
        return getQueueInfo();
    }

    @Override
    public Queue<LinkFinderInfo> getQueue() {
        return linksFindersDispatcher;
    }

    @Override
    public QueueInfo getQueueInfo() {
        Queue<LinkFinderInfo> queue = getQueue();
        LinkFinderInfo head = queue.peek();
        if (null != head) {
            LinksFinder finder = head.getFinder();
            if (null != finder) {
                return new QueueInfo(queue.size(), head.getLibrary(), head.getClarifications(),
                        head.getStatus(), finder.getParsed(), finder.getAllLinks(), finder.getPattern(), finder.getLastParsed());
            } else {
                return QueueInfo.getInstanceNoFinder(queue.size(), head.getLibrary(), head.getClarifications(),
                        head.getStatus());
            }
        }
        return QueueInfo.getEmpty();
    }

    @Override
    public synchronized Queue<LinkFinderInfo> removeFromQueue(String library) {
        for (LinkFinderInfo linkFinderInfo : linksFindersDispatcher) {
            if (linkFinderInfo.getLibrary().equals(library)) {
                LinksFinder finder = linkFinderInfo.getFinder();
                if (null != finder) {
                    finder.setShouldStop(true);
                    linkFinderInfo.setStatus(LinkFinderStatus.FINISHING);
                }
            }
        }
        return getQueue();
    }

    @Override
    public List<Library> getIndexedLibraries() {
        return dao.getIndexedLibraries();
    }

    private static List<String> parseClarifications(List<String> clarifications) {
        List<String> parsedClarifications = new LinkedList<>();

        for (String clarification : clarifications) {
            if (Utils.isClarification(clarification)) {
                parsedClarifications.add(Utils.formatClarification(clarification));
            }
        }
        return parsedClarifications;
    }

    public static void main(String[] args) throws InterruptedException {
        LibraryService libraryService = new LibraryServiceImpl();
        libraryService.addInQueue("https://shikimori.org/", Collections.singletonList("animes"));
        libraryService.addInQueue("https://pikabu.ru/", Collections.singletonList("story"));
        libraryService.addInQueue("https://habr.com/ru/all/", Collections.singletonList("post"));
        while (true) {
            System.out.println(libraryService.getQueue());
            Thread.sleep(1000);
        }
    }
}
