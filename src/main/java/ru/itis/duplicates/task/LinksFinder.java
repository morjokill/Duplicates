package ru.itis.duplicates.task;

import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleUrlPattern;
import ru.itis.duplicates.model.Link;
import ru.itis.duplicates.model.LinkStatus;
import ru.itis.duplicates.service.ArticleService;
import ru.itis.duplicates.service.impl.ArticleServiceImpl;
import ru.itis.duplicates.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

//TODO: разнести по классам
@Log
public class LinksFinder {
    private String url;
    private String rootUrl;
    private List<String> clarificationList;
    private Map<String, Link> links;
    private ExecutorService getHtmlService;
    private ExecutorService findLinksService;
    private ExecutorService siteParseService;
    private ArticleService articleService;
    private Set<String> parsedLinks;
    private Set<String> allLinks;
    private AtomicInteger handshake = new AtomicInteger(50);
    private boolean isFinder;
    private List<String> clarificationLinksFound;
    private boolean shouldStop;
    private ArticleUrlPattern articleUrlPattern;
    private long lastInRange;
    private AtomicInteger countToStop = new AtomicInteger(20);
    private AtomicLong lastParsed;

    public LinksFinder(String url, String rootUrl, List<String> clarificationList,
                       List<String> parsedLinks, ArticleUrlPattern articleUrlPattern, long lastParsed) {
        this.url = url;
        this.rootUrl = rootUrl;
        this.links = new ConcurrentHashMap<>();
        this.clarificationList = clarificationList;
        this.getHtmlService = Executors.newFixedThreadPool(2);
        this.findLinksService = Executors.newFixedThreadPool(1);
        this.siteParseService = Executors.newFixedThreadPool(5);
        this.articleService = new ArticleServiceImpl();
        this.parsedLinks = ConcurrentHashMap.newKeySet();
        if (null != parsedLinks && !parsedLinks.isEmpty()) {
            this.parsedLinks.addAll(parsedLinks);
        }
        this.allLinks = ConcurrentHashMap.newKeySet();
        if (null != parsedLinks && !parsedLinks.isEmpty()) {
            this.allLinks.addAll(parsedLinks);
        }
        this.articleUrlPattern = articleUrlPattern;
        this.lastParsed = new AtomicLong(lastParsed);
    }

    public LinksFinder(String url, String rootUrl, List<String> clarificationList, boolean isFinder) {
        this.url = url;
        this.rootUrl = rootUrl;
        this.links = new ConcurrentHashMap<>();
        this.clarificationList = clarificationList;
        this.getHtmlService = Executors.newFixedThreadPool(2);
        this.findLinksService = Executors.newFixedThreadPool(1);
        this.siteParseService = Executors.newFixedThreadPool(5);
        this.articleService = new ArticleServiceImpl();
        this.parsedLinks = ConcurrentHashMap.newKeySet();
        this.allLinks = ConcurrentHashMap.newKeySet();
        this.isFinder = isFinder;
        this.clarificationLinksFound = new LinkedList<>();
    }

    public LinksFinder(String url, String rootUrl, List<String> clarificationList,
                       ArticleUrlPattern articleUrlPattern, long lastParsed) {
        this.url = url;
        this.rootUrl = rootUrl;
        this.links = new ConcurrentHashMap<>();
        this.clarificationList = clarificationList;
        this.getHtmlService = Executors.newFixedThreadPool(2);
        this.findLinksService = Executors.newFixedThreadPool(1);
        this.siteParseService = Executors.newFixedThreadPool(5);
        this.articleService = new ArticleServiceImpl();
        this.parsedLinks = ConcurrentHashMap.newKeySet();
        this.allLinks = ConcurrentHashMap.newKeySet();
        this.articleUrlPattern = articleUrlPattern;
        this.lastParsed = new AtomicLong(lastParsed);
    }

    public void getAllLinksFromSite() throws InterruptedException {
        if (null == lastParsed || articleUrlPattern == null || !articleUrlPattern.isHasPattern()) {
            links.put(url, Link.getNewLink(url));
            allLinks.add(url);

            getHtmlService.execute(new GetHtmlTask(url));
        } else {
            lastInRange = lastParsed.longValue();
            addLinksFromRange();
        }

        if (isFinder) {
            while (clarificationLinksFound.size() < 11 && !shouldStop && countToStop.intValue() > 0) {
                System.out.println("Links: " + allLinks.size() + " " + handshake + " " + links.size());
                countToStop.decrementAndGet();
                Thread.sleep(1000);
            }
        } else {
            while (!shouldStop && countToStop.intValue() > 0) {
                if (links.size() - parsedLinks.size() < 100 && articleUrlPattern != null && articleUrlPattern.isHasPattern()) {
                    addLinksFromRange();
                }
                countToStop.decrementAndGet();
                Thread.sleep(1000);
            }
        }
        getHtmlService.shutdownNow();
        findLinksService.shutdownNow();
        siteParseService.shutdown();
        siteParseService.awaitTermination(5000, TimeUnit.SECONDS);
    }

    public class GetHtmlTask implements Runnable {
        private String url;

        GetHtmlTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            if (links.size() != 0) {
                Link link = links.get(url);
                if (null != link) {
                    if (link.getStatus() == LinkStatus.NEW) {
                        link.setStatus(LinkStatus.RESERVED);

                        try {
                            Document document = getDocumentFromUrl(url);
                            link.setHtml(document);

                            link.setStatus(LinkStatus.CHECKED);
                            countToStop.set(20);

                            if ((null == articleUrlPattern || !articleUrlPattern.isHasPattern())
                                    && !findLinksService.isShutdown() && !findLinksService.isTerminated()) {
                                findLinksService.execute(new FindLinksTask(link));
                            }
                            if (null != clarificationList && clarificationList.size() != 0) {
                                if (!isFinder) {
                                    for (String clarification : clarificationList) {
                                        if (link.getUrl().contains(clarification) && !siteParseService.isShutdown()
                                                && !siteParseService.isTerminated()) {
                                            siteParseService.execute(new SiteParseTask(link));
                                        }
                                    }
                                } else {
                                    for (String clarification : clarificationList) {
                                        if (link.getUrl().contains(clarification)) {
                                            clarificationLinksFound.add(link.getUrl());
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("FAILED LINK: " + url);
                            link.setStatus(LinkStatus.FAILED);
                        }
                    }
                }
            }
        }
    }

    public class FindLinksTask implements Runnable {
        private Link site;

        FindLinksTask(Link site) {
            this.site = site;
        }

        @Override
        public void run() {
            if (site.getStatus() == LinkStatus.CHECKED) {
                Document document = site.getHtml();
                List<String> allLinksFromSite = getAllLinksFromDocument(document);
                allLinksFromSite = filterLinksToOnlyOwn(allLinksFromSite, rootUrl, clarificationList, handshake);
                for (String linkFromSite : allLinksFromSite) {
                    if (!allLinks.contains(linkFromSite) || handshake.intValue() > 0) {
                        if (allLinks.add(linkFromSite)) {
                            handshake.decrementAndGet();
                        }

                        links.put(linkFromSite, Link.getNewLink(linkFromSite));
                        if (!getHtmlService.isShutdown() && !getHtmlService.isTerminated()) {
                            getHtmlService.execute(new GetHtmlTask(linkFromSite));
                        }
                    }
                }
                site.setStatus(LinkStatus.COLLECTED);
            }
        }
    }

    public class SiteParseTask implements Runnable {
        private Link link;

        public SiteParseTask(Link link) {
            this.link = link;
        }

        @Override
        public void run() {
            if ((link.getStatus() == LinkStatus.CHECKED || link.getStatus() == LinkStatus.COLLECTED)
                    && !parsedLinks.contains(link.getUrl())) {
                try {
                    Document document = link.getHtml();
                    String text = getTextFromDocument(document);
                    List<String> wordsFromSite = Utils.parseText(text);

                    String url = link.getUrl();
                    Article article = new Article(url, rootUrl, text);
                    articleService.saveArticle(wordsFromSite, article);

                    link.setStatus(LinkStatus.PARSED);
                    if (articleUrlPattern != null && articleUrlPattern.isHasPattern()) {
                        lastParsed.set(Long.parseLong(link.getUrl().replace(articleUrlPattern.getBeforeRange(), "")
                                .replace(articleUrlPattern.getAfterRange(), "")));
                    }
                    links.remove(url);
                    parsedLinks.add(url);
                } catch (Exception e) {
                    link.setStatus(LinkStatus.FAILED);
                }
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    private static String getTextFromDocument(Document document) {
        Element body = document.body();
        String removeTags = "a,button,img,input,menu,nav,textarea,time,video,form";
        body.select(removeTags).remove();
        Elements allElements = body.getAllElements();
        List<Element> collect = allElements.stream().filter(element -> element.hasText() && element.text().length() < 1000).collect(Collectors.toList());
        for (Element element : collect) {
            element.remove();
        }
        return body.text();
    }

    //TODO: может лучше сет?
    private static List<String> getAllLinksFromDocument(Document document) {
        List<String> links = new LinkedList<>();

        Elements hrefElements = document.select("a[href]");
        for (Element href : hrefElements) {
            String link = href.attr("href");
            links.add(link);
        }

        return links;
    }

    private static Document getDocumentFromUrl(String url) throws IOException {
        return Jsoup.connect(url).get();
    }

    //TODO: быстрее будет регуляркой?
    private static List<String> filterLinksToOnlyOwn(List<String> links, String rootUrl, List<String> clarificationList, AtomicInteger handshake) {
        return links.stream().filter(s -> {
            if (s.contains(rootUrl)) {
                try {
                    if (Utils.getRootUrl(s).equals(rootUrl)) {
                        if (null != clarificationList && clarificationList.size() != 0) {
                            for (String clarification : clarificationList) {
                                if (s.contains(clarification) || handshake.intValue() > 0) {
                                    return true;
                                }
                            }
                        } else {
                            return true;
                        }
                    }
                } catch (MalformedURLException e) {
                    System.out.println("BAD URL: " + s);
                }
            }
            return false;
        }).collect(Collectors.toList());
    }

    public void setShouldStop(boolean shouldStop) {
        this.shouldStop = shouldStop;
    }

    public List<String> getClarificationLinksFound() {
        return clarificationLinksFound;
    }

    private void addLinksFromRange() {
        String beforeRange = articleUrlPattern.getBeforeRange();
        String afterRange = articleUrlPattern.getAfterRange();
        for (int i = 0; i < 100; i++) {
            lastInRange++;
            String link = beforeRange + lastInRange + afterRange;
            allLinks.add(link);
            links.put(link, Link.getNewLink(link));
            if (!getHtmlService.isShutdown() && !getHtmlService.isTerminated()) {
                getHtmlService.execute(new GetHtmlTask(link));
            }
        }
    }

    public AtomicLong getLastParsed() {
        return lastParsed;
    }

    public long getParsed() {
        return parsedLinks.size();
    }

    public long getAllLinks() {
        return allLinks.size();
    }

    public ArticleUrlPattern getPattern() {
        return articleUrlPattern;
    }

    @Override
    public String toString() {
        return "LinksFinder{" +
                "parsedLinks=" + parsedLinks.size() +
                ", allLinks=" + allLinks.size() +
                '}';
    }
}
