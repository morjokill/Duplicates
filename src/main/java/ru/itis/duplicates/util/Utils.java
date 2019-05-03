package ru.itis.duplicates.util;

import lombok.extern.java.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class Utils {
    private static Pattern clarificationPattern = Pattern.compile("^([\\/\\\\][-a-zA-Z0-9@:%._+~#=]{2,256}" +
            "[\\/\\\\])|([-a-zA-Z0-9@:%._+~#=]{2,256})$");

    public static double calculateWeight(double tf, double rIdf) {
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

    public static double calculateFrequency(int wordCount, int allWordsCount) {
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

    public static void main(String[] args) {
        System.out.println("asd");
    }
}
