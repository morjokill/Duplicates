package ru.itis.duplicates.service.impl;

import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.itis.duplicates.app.Application;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Word;
import ru.itis.duplicates.service.DuplicatesService;
import ru.itis.duplicates.util.Utils;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class DuplicatesServiceImpl implements DuplicatesService {
    private final static String stopWordsFilePath = "src\\main\\resources\\stop_words.txt";
    private static final int MIN_WORDS_SIZE = 3;

    private Dao dao;

    public DuplicatesServiceImpl(Dao dao) {
        this.dao = dao;
    }

    public DuplicatesServiceImpl() {
        this.dao = new DaoImpl();
    }

    @Override
    public void saveArticle(List<String> words, Article article) {
        String articleUrl = article.getUrl();
        Map<String, Integer> mapOfWordsCountForDocument = calculateWordsCount(words);
        int wordsCount = words.size();
        double maxFreq = calculateMaxFrequencyInDocument(mapOfWordsCountForDocument, wordsCount);

        article.setWordsCount(wordsCount);
        dao.saveArticle(article);
        dao.updateLibraryWordsCount(article.getLibrary(), wordsCount);

        List<Word> wordsToSave = new LinkedList<>();
        List<ArticleWord> articleWordsToSave = new LinkedList<>();
        for (String word : mapOfWordsCountForDocument.keySet()) {
            Integer wordCount = mapOfWordsCountForDocument.get(word);
            double freq = Utils.calculateFrequency(wordCount, wordsCount);
            double tf = Utils.calculateTf(freq, maxFreq);

            wordsToSave.add(new Word(word, freq));
            articleWordsToSave.add(new ArticleWord(articleUrl, word, wordCount, tf));
            System.out.println("'" + word + "'. freq: " + freq + ". tf:" + tf);
        }

        dao.saveWords(wordsToSave);
        dao.saveArticleWords(articleWordsToSave);

        dao.recalculateWeight();
    }

    private static Map<String, Integer> calculateWordsCount(List<String> words) {
        Map<String, Integer> mapOfWordsCount = new HashMap<>();
        for (String word : words) {
            mapOfWordsCount.merge(word, 1, (oldValue, one) -> oldValue + one);
        }
        return mapOfWordsCount;
    }

    private static double calculateMaxFrequencyInDocument(Map<String, Integer> mapOfWordsCount, int allWordsCount) {
        if (null != mapOfWordsCount && mapOfWordsCount.size() != 0) {
            OptionalInt max = mapOfWordsCount.values().stream().mapToInt(Integer::intValue).max();
            return max.isPresent() ? (double) max.getAsInt() / allWordsCount : 0;
        }
        return 0;
    }

    public static List<String> parseText(String text) throws MyStemApplicationException, IOException {
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
        return Utils.readFile(stopWordsFilePath);
    }

    private static List<String> removeStopWordsFromWordsList(List<String> wordsList, Set<String> stopWords) {
        wordsList.removeAll(stopWords);
        return wordsList;
    }

    private static List<String> removeShortWords(List<String> words, int minWordsSize) {
        return words.stream().filter(s -> s.length() > minWordsSize).collect(Collectors.toList());
    }

    private static List<String> getAllLinksFromUrl(String url) throws IOException {
        List<String> links = new LinkedList<>();
        Document doc = Jsoup.connect(url).get();

        Elements hrefElements = doc.select("a[href]");
        for (Element href : hrefElements) {
            String link = href.attr("href");
            System.out.println("href : " + link);
            links.add(link);
        }

        return links;
    }

    private static List<String> filterLinksToOnlyOwn(List<String> links, String url) {
        return links.stream().filter(s -> s.contains(url)).collect(Collectors.toList());
    }

    private static String getRootUrl(String link) throws MalformedURLException {
        URL url = new URL(link);
        return url.getProtocol() + "://" + url.getHost();
    }

    private static String getTextFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        return doc.body().text();
    }

    public static void main(String[] args) {
        String url = "https://habr.com/ru/top/";
    }
}
