package ru.itis.duplicates.service.impl;

import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Word;
import ru.itis.duplicates.service.DuplicatesService;
import ru.itis.duplicates.util.Utils;

import java.util.*;

public class DuplicatesServiceImpl implements DuplicatesService {
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

        List<Word> wordsToSave = new LinkedList<>();
        List<ArticleWord> articleWordsToSave = new LinkedList<>();
        for (String word : mapOfWordsCountForDocument.keySet()) {
            double freq = Utils.calculateFrequency(mapOfWordsCountForDocument.get(word), wordsCount);
            double tf = Utils.calculateTf(freq, maxFreq);

            wordsToSave.add(new Word(word, freq));
            articleWordsToSave.add(new ArticleWord(articleUrl, word, freq, tf));
            System.out.println("'" + word + "'. freq: " + freq + ". tf:" + tf);
        }

        dao.saveWords(wordsToSave);
        dao.saveArticleWords(articleWordsToSave);
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

    public static void main(String[] args) {
        String parseString = "Краткое теоретическое введение Основная\n" +
                "идея     состоит в сравнении\n" +
                "двух способов подсчета количества информации в\n" +
                "смысле определения К Шеннона содержащейся в\n" +
                "сообщении о том что данное слово входит в\n" +
                "некоторый документ по меньшей мере один раз\n" +
                "Первый способ статистический это обычный  \n" +
                "   Второй способ теоретический основан\n" +
                "на модели распределения Пуассона\n" +
                "предполагающей что слова в коллекции\n" +
                "документов распределяются случайным и\n" +
                "независимым образом равномерно рассеиваясь с\n" +
                "некоторой средней плотностью ".trim();
        List<String> words = Arrays.asList(parseString.split(" "));
        DuplicatesServiceImpl duplicatesService = new DuplicatesServiceImpl();
        duplicatesService.saveArticle(words,
                new Article("megalib.com/article/1", "megalib.com", parseString));

    }
}
