package ru.itis.duplicates.task;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class LinksFinder {
    private String url;
    private String rootUrl;
    private List<String> clarificationList;
    private Map<String, LinkStatus> links;
    private ExecutorService executorService;

    public LinksFinder(String url, List<String> clarificationList) throws MalformedURLException {
        this.url = url;
        this.rootUrl = getRootUrl(url);
        this.links = new ConcurrentHashMap<>();
        this.clarificationList = clarificationList;
        //TODO: вынести
        this.executorService = Executors.newFixedThreadPool(10);
    }

    //TODO: когда останавливаться?
    //TODO: при добавлении ссылки нужна проверка на доступность
    public void getAllLinksFromSite() throws InterruptedException {
        links.put(url, LinkStatus.NEW);

        executorService.execute(new LinksFindTask(url));
        while (true) {
            System.out.println(links.size());
            Thread.sleep(100);
        }
    }

    private enum LinkStatus {
        NEW, RESERVED, PARSED, FAILED
    }

    public class LinksFindTask implements Runnable {
        private String link;

        LinksFindTask(String link) {
            this.link = link;
        }

        //TODO: как-то сохранять состояние, иначе все если рухнет, то все гг
        //TODO: ввести уточнение? DA
        //TODO: можно остановить в любой момент и все добавиться!
        @Override
        public void run() {
            if (links.size() != 0) {
                LinkStatus linkStatus = links.get(link);
                if (linkStatus == LinkStatus.NEW) {
                    links.put(link, LinkStatus.RESERVED);

                    try {
                        List<String> allLinksFromSite = getAllLinksFromUrl(link);
                        allLinksFromSite = filterLinksToOnlyOwn(allLinksFromSite, rootUrl, clarificationList);
                        for (String linkFromSite : allLinksFromSite) {
                            if (!links.containsKey(linkFromSite)) {
                                links.put(linkFromSite, LinkStatus.NEW);
                                System.out.println("href : " + linkFromSite);
                                executorService.execute(new LinksFindTask(linkFromSite));
                            }
                        }
                        links.put(link, LinkStatus.PARSED);
                    } catch (IOException e) {
                        links.put(link, LinkStatus.FAILED);
                    }
                }
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public Map<String, LinkStatus> getLinks() {
        return links;
    }

    //TODO: может лучше сет?
    private static List<String> getAllLinksFromUrl(String url) throws IOException {
        List<String> links = new LinkedList<>();
        Document doc = Jsoup.connect(url).get();

        Elements hrefElements = doc.select("a[href]");
        for (Element href : hrefElements) {
            String link = href.attr("href");
            links.add(link);
        }

        return links;
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
