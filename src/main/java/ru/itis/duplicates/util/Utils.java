package ru.itis.duplicates.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Utils {
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

    public static List<String> readFile(String file) throws IOException {
        List<String> lines = new LinkedList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new java.io.FileReader(file))) {
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
}
