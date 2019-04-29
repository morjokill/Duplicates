package ru.itis.duplicates.task;

import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.itis.duplicates.app.Application;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.Link;
import ru.itis.duplicates.model.LinkStatus;
import ru.itis.duplicates.service.DuplicatesService;
import ru.itis.duplicates.service.impl.DuplicatesServiceImpl;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

//TODO: разнести по классам
public class LinksFinder {
    private String url;
    private String rootUrl;
    private List<String> clarificationList;
    private Map<String, Link> links;
    private ExecutorService linkFindService;
    private ExecutorService siteParseService;
    private DuplicatesService duplicatesService;

    public LinksFinder(String url, List<String> clarificationList) throws MalformedURLException {
        this.url = url;
        this.rootUrl = getRootUrl(url);
        this.links = new ConcurrentHashMap<>();
        this.clarificationList = clarificationList;
        //TODO: вынести
        this.linkFindService = Executors.newFixedThreadPool(2);
        this.siteParseService = Executors.newFixedThreadPool(8);
        //TODO: DI
        this.duplicatesService = new DuplicatesServiceImpl();
    }

    //TODO: когда останавливаться?
    //TODO: при добавлении ссылки нужна проверка на доступность
    //TODO: зачем 2 скачивать страницу? 1 раз скачивать надо и в 2 потока обрабатывать. 2 раза скачивать - хуйня
    public void getAllLinksFromSite() throws InterruptedException {
        links.put(url, Link.getNewLink(url));

        linkFindService.execute(new LinksFindTask(url));
        while (true) {
            System.out.println(links.size());
            Thread.sleep(500);
        }
    }

    public class LinksFindTask implements Runnable {
        private String url;

        LinksFindTask(String url) {
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
                if (link.getStatus() == LinkStatus.NEW) {
                    link.setStatus(LinkStatus.RESERVED);

                    try {
                        Document document = getDocumentFromUrl(url);
                        List<String> allLinksFromSite = getAllLinksFromDocument(document);
                        allLinksFromSite = filterLinksToOnlyOwn(allLinksFromSite, rootUrl, clarificationList);
                        String textFromSite = getTextFromDocument(document);
                        for (String linkFromSite : allLinksFromSite) {
                            if (!links.containsKey(linkFromSite)) {
                                links.put(linkFromSite, Link.getNewLink(linkFromSite));
                                linkFindService.execute(new LinksFindTask(linkFromSite));
                            }
                        }
                        link.setText(textFromSite);
                        link.setStatus(LinkStatus.COLLECTED);
                        siteParseService.execute(new SiteParseTask(link, duplicatesService, rootUrl));
                    } catch (IOException e) {
                        link.setStatus(LinkStatus.FAILED);
                    }
                }
            }
        }
    }

    public static class SiteParseTask implements Runnable {
        private final static String stopWordsFilePath = "src\\main\\resources\\stop_words.txt";
        private static final int MIN_WORDS_SIZE = 3;
        private Link link;
        private DuplicatesService duplicatesService;
        private String rootUrl;

        public SiteParseTask(Link link, DuplicatesService duplicatesService, String rootUrl) {
            this.link = link;
            this.duplicatesService = duplicatesService;
            this.rootUrl = rootUrl;
        }

        @Override
        public void run() {
            if (link.getStatus() == LinkStatus.COLLECTED) {
                try {
                    String text = link.getText();
                    List<String> wordsFromSite = parseText(text);

                    Article article = new Article(link.getUrl(), rootUrl, text);
                    duplicatesService.saveArticle(wordsFromSite, article);

                    link.setStatus(LinkStatus.PARSED);
                } catch (Exception e) {
                    //TODO: log?
                    link.setStatus(LinkStatus.FAILED);
                }
            }
        }

        private static List<String> parseText(String text) throws MyStemApplicationException, IOException {
            List<String> resultList = new LinkedList<>();

            if (!Strings.isEmpty(text)) {
                List<String> stemmedWords = stemLine(text);
                resultList = removeStopWordsFromWordsList(stemmedWords, getStopWords());
                resultList = removeShortWords(resultList, MIN_WORDS_SIZE);

                return resultList;
            }
            return resultList;
        }

        private static List<String> stemLine(String line) throws MyStemApplicationException {
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

        private static Set<String> getStopWords() throws IOException {
            return Application.getStopWords();
        }

        private static List<String> removeStopWordsFromWordsList(List<String> wordsList, Set<String> stopWords) {
            wordsList.removeAll(stopWords);
            return wordsList;
        }

        private static List<String> removeShortWords(List<String> words, int minWordsSize) {
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
    private static List<String> filterLinksToOnlyOwn(List<String> links, String url, List<String> clarificationList) {
        return links.stream().filter(s -> {
            try {
                if (s.contains(url)) {
                    if (getRootUrl(s).equals(url)) {
                        if (null != clarificationList && clarificationList.size() != 0) {
                            for (String clarification : clarificationList) {
                                if (s.contains(clarification)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            } catch (MalformedURLException e) {
                return false;
            }
            return false;
        }).collect(Collectors.toList());
    }

    private static String getRootUrl(String link) throws MalformedURLException {
        URL url = new URL(link);
        return url.getProtocol() + "://" + url.getHost();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        LinksFinder linksFinder = new LinksFinder("https://habr.com/ru/top/", Arrays.asList("/post/", "/company/"));
        linksFinder.getAllLinksFromSite();
    }
}
