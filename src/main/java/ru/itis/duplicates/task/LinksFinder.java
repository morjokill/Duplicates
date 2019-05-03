package ru.itis.duplicates.task;

import lombok.extern.java.Log;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.itis.duplicates.app.Application;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ClarificationRange;
import ru.itis.duplicates.model.Link;
import ru.itis.duplicates.model.LinkStatus;
import ru.itis.duplicates.service.ArticleService;
import ru.itis.duplicates.service.impl.ArticleServiceImpl;
import ru.itis.duplicates.util.Utils;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//TODO: разнести по классам
@Log
public class LinksFinder {
    private String url;
    private String rootUrl;
    private List<String> clarificationList;
    private ClarificationRange clarificationRange;
    private Map<String, Link> links;
    private ExecutorService getHtmlService;
    private ExecutorService findLinksService;
    private ExecutorService siteParseService;
    private ArticleService articleService;
    private Set<String> parsedLinks;
    private Set<String> allLinks;
    //TODO: четкая тема
    private AtomicInteger handshake = new AtomicInteger(50);

    //TODO: 50 вложений, чтобы найти новые?
    //TODO: когда закончит собирать ссылки - все силы на парс
    //TODO: написать оптимизатор, чтобы ссылки не собирал, пока парс не догонит!
    public LinksFinder(String url, String rootUrl, List<String> clarificationList, List<String> parsedLinks) {
        this.url = url;
        this.rootUrl = rootUrl;
        this.links = new ConcurrentHashMap<>();
        this.clarificationList = clarificationList;
        //TODO: вынести
        this.getHtmlService = Executors.newFixedThreadPool(2);
        this.findLinksService = Executors.newFixedThreadPool(1);
        this.siteParseService = Executors.newFixedThreadPool(5);
        //TODO: DI
        this.articleService = new ArticleServiceImpl();
        this.parsedLinks = ConcurrentHashMap.newKeySet();
        if (null != parsedLinks) {
            this.parsedLinks.addAll(parsedLinks);
        }
        this.allLinks = ConcurrentHashMap.newKeySet();
        if (null != parsedLinks) {
            this.allLinks.addAll(parsedLinks);
        }
    }

    //TODO: когда останавливаться?
    //TODO: при добавлении ссылки нужна проверка на доступность
    //TODO: зачем 2 скачивать страницу? 1 раз скачивать надо и в 2 потока обрабатывать. 2 раза скачивать - хуйня
    public void getAllLinksFromSite() throws InterruptedException {
        links.put(url, Link.getNewLink(url));
        allLinks.add(url);

        getHtmlService.execute(new GetHtmlTask(url));
        while (true) {
            System.out.println("Parsed: " + parsedLinks.size() + " / " + allLinks.size() + " " + handshake + " " + links.size());
            Thread.sleep(500);
        }
    }

    public class GetHtmlTask implements Runnable {
        private String url;

        GetHtmlTask(String url) {
            this.url = url;
        }

        //todo: надо писать в бд со статусом
        //TODO: как-то сохранять состояние, иначе все если рухнет, то все гг
        //TODO: ввести уточнение? DA
        //TODO: можно остановить в любой момент и все добавиться!
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

                            findLinksService.execute(new FindLinksTask(link));
                            if (null != clarificationList && clarificationList.size() != 0) {
                                for (String clarification : clarificationList) {
                                    if (link.getUrl().contains(clarification)) {
                                        siteParseService.execute(new SiteParseTask(link));
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
                        getHtmlService.execute(new GetHtmlTask(linkFromSite));
                    }
                }
                site.setStatus(LinkStatus.COLLECTED);
            }
        }
    }

    public class SiteParseTask implements Runnable {
        private static final int MIN_WORDS_SIZE = 3;
        private Link link;

        public SiteParseTask(Link link) {
            this.link = link;
        }

        @Override
        public void run() {
            if (link.getStatus() == LinkStatus.CHECKED && !parsedLinks.contains(link.getUrl())) {
                try {
                    Document document = link.getHtml();
                    String text = getTextFromDocument(document);
                    List<String> wordsFromSite = parseText(text);

                    String url = link.getUrl();
                    Article article = new Article(url, rootUrl, text);
                    articleService.saveArticle(wordsFromSite, article);

                    link.setStatus(LinkStatus.PARSED);
                    //TODO: временно
                    links.remove(url);
                    parsedLinks.add(url);
                } catch (Exception e) {
                    //TODO: log?
                    link.setStatus(LinkStatus.FAILED);
                }
            }
        }

        private List<String> parseText(String text) throws MyStemApplicationException, IOException {
            List<String> resultList = new LinkedList<>();

            if (!Strings.isEmpty(text)) {
                List<String> stemmedWords = stemLine(text);
                resultList = removeStopWordsFromWordsList(stemmedWords, getStopWords());
                resultList = removeShortWords(resultList, MIN_WORDS_SIZE);

                return resultList;
            }
            return resultList;
        }

        private List<String> stemLine(String line) throws MyStemApplicationException {
            List<String> stemmedWords = new LinkedList<>();
            //TODO: тоже ток 1 раз?
            final Iterable<Info> result =
                    JavaConversions.asJavaIterable(
                            Application.getMyStemAnalyzer()
                                    .analyze(Request.apply(line))
                                    .info()
                                    .toIterable());

            for (final Info info : result) {
                Option<String> lex = info.lex();
                if (Objects.nonNull(lex) && lex.isDefined()) {
                    stemmedWords.add(lex.get());
                }
            }

            return stemmedWords;
        }

        private Set<String> getStopWords() throws IOException {
            return Application.getStopWords();
        }

        private List<String> removeStopWordsFromWordsList(List<String> wordsList, Set<String> stopWords) {
            wordsList.removeAll(stopWords);
            return wordsList;
        }

        private List<String> removeShortWords(List<String> words, int minWordsSize) {
            return words.stream().filter(s -> s.length() > minWordsSize).collect(Collectors.toList());
        }
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    //TODO:validation
    private static String getTextFromDocument(Document document) {
        return document.body().text();
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
}
