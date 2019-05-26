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
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
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
            LocalDateTime lastVacuum = null;
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
                } else {
                    LocalDateTime now = LocalDateTime.now();
                    if (null == lastVacuum || (lastVacuum.until(now, ChronoUnit.MINUTES) >= 5 &&
                            (linksFindersDispatcher.isEmpty() || linksFindersDispatcher == null ||
                                    lastVacuum.until(now, ChronoUnit.MINUTES) >= 60))) {
                        System.out.println("PERFORMING VACUUM");
                        lastVacuum = now;
                        dao.vacuumWordArticleTable();
                        System.out.println("VACUUM DONE");
                    }
                }
            }
        }
    }

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

            LocalDateTime before = LocalDateTime.now();
            dao.recalculateWeight(rootUrl, libraryFromDb.getWordsCount());
            System.out.println("RECALCULATE TOOK: " + before.until(LocalDateTime.now(), ChronoUnit.MILLIS) + " ms");

            LocalDateTime beforeSign = LocalDateTime.now();
            List<String> articlesFromLibrary = dao.getArticlesFromLibrary(rootUrl);
            dao.updateArticlesSignatures(articlesFromLibrary);
            System.out.println("SIGNATURE TOOK: " + beforeSign.until(LocalDateTime.now(), ChronoUnit.MILLIS) + " ms");

            info.setStatus(LinkFinderStatus.FINISHED);

        } catch (Exception e) {
            System.out.println("links finder gg: " + e);
        }
    }

    @Override
    public synchronized String addInQueue(String library, List<String> clarifications) {
        boolean urlReachable = Utils.isUrlReachable(library, 1000);
        String uuid;
        if (urlReachable) {
            System.out.println("URL: " + library + " is reachable");
            uuid = Utils.getTimeBasedUuid();
            LinkFinderInfo newInstance = LinkFinderInfo.getNewInstance(null, library, clarifications, uuid);
            boolean isContains = linksFindersDispatcher.contains(newInstance);
            if (!isContains) {
                linksFindersDispatcher.add(newInstance);
                return uuid;
            } else {
                System.out.println("URL: " + library + " is already in queue");
                return null;
            }
        } else {
            System.out.println("URL: " + library + " is NOT reachable");
        }
        return null;
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
    public synchronized boolean removeFromQueue(String uuid) {
        for (LinkFinderInfo linkFinderInfo : linksFindersDispatcher) {
            if (linkFinderInfo.getUuid().equals(uuid)) {
                LinksFinder finder = linkFinderInfo.getFinder();
                if (null != finder) {
                    finder.setShouldStop(true);
                    linkFinderInfo.setStatus(LinkFinderStatus.FINISHING);
                    return true;
                }
            }
        }
        return false;
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
}
