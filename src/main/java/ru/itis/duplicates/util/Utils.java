package ru.itis.duplicates.util;

public class Utils {
    public static double calculateWeight(double tf, double rIdf) {
        return tf * rIdf;
    }

    public static double calculateTf(double frequencyOfLocalDocument, double maxFrequencyOfCollection) {
        return 0.5 + 0.5 * frequencyOfLocalDocument / maxFrequencyOfCollection;
    }

    public static double calculateRIdf(double idf, double pIdf) {
        return idf - pIdf;
    }

    public static double calculateIdf(int articlesWithWordCount, int allArticlesCount) {
        return Math.log((double) articlesWithWordCount / allArticlesCount) * (-1);
    }

    public static double calculatePIdf(int wordSumInCollection, int allArticlesCount) {
        return Math.log(1 - Math.exp((-1) * (double) wordSumInCollection / allArticlesCount)) * (-1);
    }

    public static double calculateFrequency(int wordCount, int allWordsCount) {
        return (double) wordCount / allWordsCount;
    }
}
