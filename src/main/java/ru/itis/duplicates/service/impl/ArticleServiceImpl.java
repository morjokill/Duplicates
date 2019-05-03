package ru.itis.duplicates.service.impl;

import lombok.extern.java.Log;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Word;
import ru.itis.duplicates.service.ArticleService;
import ru.itis.duplicates.util.Utils;

import java.util.*;

//TODO: сделать проверку exists
@Log
public class ArticleServiceImpl implements ArticleService {
    private Dao dao;

    public ArticleServiceImpl(Dao dao) {
        this.dao = dao;
    }

    public ArticleServiceImpl() {
        this.dao = new DaoImpl();
    }

    @Override
    public void saveArticle(List<String> words, Article article) {
        String articleUrl = article.getUrl();

        if (!dao.isArticleExists(articleUrl)) {
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

                wordsToSave.add(new Word(word, wordCount, article.getLibrary()));
                articleWordsToSave.add(new ArticleWord(articleUrl, word, wordCount, tf));
            }

            dao.saveWords(wordsToSave);
            dao.saveArticleWords(articleWordsToSave);
        } else {
            //TODO:
            System.out.println("article exists: " + articleUrl);
        }
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
        String url = "https://habr.com/ru/top/";
    }
}
