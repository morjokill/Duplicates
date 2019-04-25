package ru.itis.duplicates.util;

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
}
