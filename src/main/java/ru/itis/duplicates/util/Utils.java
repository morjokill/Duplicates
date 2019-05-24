package ru.itis.duplicates.util;

import lombok.extern.java.Log;
import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.util.Strings;
import ru.itis.duplicates.app.Application;
import ru.itis.duplicates.model.ArticleUrlPattern;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

@Log
public class Utils {
    private static Pattern clarificationPattern = Pattern.compile("^([\\/\\\\][-a-zA-Z0-9@:%._+~#=]{2,256}" +
            "[\\/\\\\])|([-a-zA-Z0-9@:%._+~#=]{2,256})$");
    private static Pattern rangePattern = Pattern.compile("^.*\\/([\\d]+)\\/?.*$");

    public static List<String> parseText(String text) throws MyStemApplicationException, IOException {
        int minWordsSize = 3;
        List<String> resultList = new LinkedList<>();

        if (!Strings.isEmpty(text)) {
            List<String> stemmedWords = stemLine(text);
            resultList = removeStopWordsFromWordsList(stemmedWords, getStopWords());
            resultList = removeShortWords(resultList, minWordsSize);

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
        return Application.getStopWords();
    }

    private static List<String> removeStopWordsFromWordsList(List<String> wordsList, Set<String> stopWords) {
        wordsList.removeAll(stopWords);
        return wordsList;
    }

    private static List<String> removeShortWords(List<String> words, int minWordsSize) {
        return words.stream().filter(s -> s.length() > minWordsSize).collect(Collectors.toList());
    }

    public static double calculateWeight(double tf, double rIdf) {
        if (Double.compare(rIdf, 0) == 0) {
            return -1337;
        }
        return tf * rIdf;
    }

    public static double calculateTf(double frequencyOfLocalDocument, double maxFrequencyInDocument) {
        if (maxFrequencyInDocument != 0) {
            return 0.5 + 0.5 * frequencyOfLocalDocument / maxFrequencyInDocument;
        }
        return 0;
    }

    public static double calculateRIdf(double idf, double pIdf) {
        return idf - pIdf;
    }

    public static double calculateIdf(int articlesWithWordCount, int allArticlesCount) {
        if (allArticlesCount != 0 && articlesWithWordCount != 0) {
            return Math.log((double) articlesWithWordCount / allArticlesCount) * (-1);
        }
        return 0;
    }

    public static double calculatePIdf(double wordSumFreqInCollection, int allArticlesCount) {
        if (allArticlesCount != 0 && wordSumFreqInCollection != 0) {
            return Math.log(1 - Math.exp((-1) * wordSumFreqInCollection / allArticlesCount)) * (-1);
        }
        return 0;
    }

    public static double calculateFrequency(Long wordCount, int allWordsCount) {
        if (allWordsCount != 0) {
            return (double) wordCount / allWordsCount;
        }
        return 0;
    }

    public static Set<String> readFile(String file) throws IOException {
        Set<String> lines = new HashSet<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while (Objects.nonNull(line = bufferedReader.readLine())) {
                lines.add(line);
            }
            return lines;
        } catch (IOException ioe) {
            System.out.println("Error reading file: " + file + " ." + ioe);
            throw ioe;
        }
    }

    public static boolean isClarification(String clarification) {
        Matcher matcher = clarificationPattern.matcher(clarification);
        return matcher.matches();
    }

    public static String formatClarification(String clarification) {
        if (!clarification.contains("/") && !clarification.contains("\\")) {
            clarification = "/" + clarification + "/";
        }
        return clarification;
    }

    public static String getRootUrl(String link) throws MalformedURLException {
        URL url = new URL(link);
        return url.getProtocol() + "://" + url.getHost();
    }

    public static long calculateCRC32(String line) {
        CRC32 crc32Instance = new CRC32();
        crc32Instance.update(line.getBytes(Charsets.UTF_8));
        return crc32Instance.getValue();
    }

    public static Map<String, Long> calculateWordsCount(List<String> words) {
        return words.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    }

    public static double calculateMaxFrequencyInDocument(Map<String, Long> mapOfWordsCount, int allWordsCount) {
        if (null != mapOfWordsCount && mapOfWordsCount.size() != 0) {
            OptionalLong max = mapOfWordsCount.values().stream().mapToLong(Long::longValue).max();
            return max.isPresent() ? (double) max.getAsLong() / allWordsCount : 0;
        }
        return 0;
    }

    public static ArticleUrlPattern getArticleUrlPattern(List<String> exampleUrls) {
        if (Objects.nonNull(exampleUrls) && exampleUrls.size() >= 2) {
            int count = 0;
            String beforeRange = null;
            String afterRange = null;
            for (String exampleUrl : exampleUrls) {
                Matcher rangeMatcher = rangePattern.matcher(exampleUrl);
                if (rangeMatcher.find()) {
                    if (null == beforeRange) {
                        beforeRange = exampleUrl.substring(0, rangeMatcher.start(1));
                        afterRange = exampleUrl.substring(rangeMatcher.end(1), exampleUrl.length());
                        count = 1;
                    } else {
                        String beforeRangeCandidate = exampleUrl.substring(0, rangeMatcher.start(1));
                        String afterRangeCandidate = exampleUrl.substring(rangeMatcher.end(1), exampleUrl.length());
                        if (!beforeRange.equals(beforeRangeCandidate) || !afterRange.equals(afterRangeCandidate)) {
                            if (beforeRange.equals(beforeRangeCandidate)) {
                                afterRange = afterRange.length() < afterRangeCandidate.length() ? afterRange : afterRangeCandidate;
                                afterRange = afterRange.contains("/") ? afterRange : "/";
                                count++;
                            } else {
                                if (beforeRange.length() > beforeRangeCandidate.length()) {
                                    beforeRange = beforeRangeCandidate;
                                    count = 1;
                                }
                            }
                        } else {
                            count++;
                        }
                    }
                }
            }
            return count > 1 ? new ArticleUrlPattern(beforeRange, afterRange) : ArticleUrlPattern.getNoPatternInstance();
        }
        return ArticleUrlPattern.getNoPatternInstance();
    }

    public static boolean isUrlReachable(String url, int timeout) {
        try (Socket socket = new Socket()) {
            URL urlInstance = new URL(url);
            String host = urlInstance.getHost();
            int port = urlInstance.getPort() == -1 ? 80 : urlInstance.getPort();

            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
