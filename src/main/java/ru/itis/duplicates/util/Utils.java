package ru.itis.duplicates.util;

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.java.Log;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import ru.itis.duplicates.app.Application;
import ru.itis.duplicates.model.ArticleUrlPattern;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.*;
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
    private static Pattern notCyrillicPattern = Pattern.compile("(?!([а-яА-ЯёЁ\\s]+)).");

    public static List<String> parseText(String text) throws MyStemApplicationException, IOException {
        Matcher matcher = notCyrillicPattern.matcher(text);
        text = matcher.replaceAll("");
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

    public static String getTimeBasedUuid() {
        return UUIDs.timeBased().toString();
    }

    public static String getFileExtension(String fileName) {
        if (!Strings.isEmpty(fileName)) {
            return FilenameUtils.getExtension(fileName);
        }
        return "";
    }

    public static String getStringFromIS(InputStream inputStream) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, Charsets.UTF_8);
        return writer.toString();
    }

    public static String getStringFromPdfIS(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            if (!document.isEncrypted()) {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                PDFTextStripper tStripper = new PDFTextStripper();
                return tStripper.getText(document);
            } else {
                throw new IOException("Document is encrypted");
            }
        }
    }

    public static String getStringFromDocIS(InputStream inputStream) throws IOException {
        XWPFDocument document = new XWPFDocument(inputStream);
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph para : paragraphs) {
            sb.append(para.getText());
        }
        return sb.toString();
    }

    public static double getLinesSimilarity(String firstLine, String secondLine) {
        int[] Di_1 = new int[secondLine.length() + 1];
        int[] Di = new int[secondLine.length() + 1];

        for (int j = 0; j <= secondLine.length(); j++) {
            Di[j] = j;
        }
        for (int i = 1; i <= firstLine.length(); i++) {
            System.arraycopy(Di, 0, Di_1, 0, Di_1.length);

            Di[0] = i;
            for (int j = 1; j <= secondLine.length(); j++) {
                int cost = (firstLine.charAt(i - 1) != secondLine.charAt(j - 1)) ? 1 : 0;
                Di[j] = Math.min(Math.min(Di_1[j] + 1, Di[j - 1] + 1), Di_1[j - 1] + cost);
            }
        }
        return (1 - (double) Di[Di.length - 1] / firstLine.length()) * 100;
    }
}
