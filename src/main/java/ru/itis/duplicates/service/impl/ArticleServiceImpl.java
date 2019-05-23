package ru.itis.duplicates.service.impl;

import lombok.extern.java.Log;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Word;
import ru.itis.duplicates.service.ArticleService;
import ru.itis.duplicates.util.Utils;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Log
public class ArticleServiceImpl implements ArticleService {
    private Dao dao;

    private ArticleServiceImpl(Dao dao) {
        this.dao = dao;
    }

    public ArticleServiceImpl() {
        this(new DaoImpl());
    }

    @Override
    public void saveArticle(List<String> words, Article article) {
        if (words != null && words.size() != 0) {
            String articleUrl = article.getUrl();

            if (!dao.isArticleExists(articleUrl)) {
                Map<String, Long> mapOfWordsCountForDocument = Utils.calculateWordsCount(words);
                int wordsCount = words.size();
                double maxFreq = Utils.calculateMaxFrequencyInDocument(mapOfWordsCountForDocument, wordsCount);

                article.setWordsCount(wordsCount);
                dao.saveArticle(article);
                dao.updateLibraryWordsCount(article.getLibrary(), wordsCount);

                List<Word> wordsToSave = new LinkedList<>();
                List<ArticleWord> articleWordsToSave = new LinkedList<>();
                for (String word : mapOfWordsCountForDocument.keySet()) {
                    Long wordCount = mapOfWordsCountForDocument.get(word);
                    double freq = Utils.calculateFrequency(wordCount, wordsCount);
                    double tf = Utils.calculateTf(freq, maxFreq);

                    wordsToSave.add(new Word(word, wordCount, article.getLibrary()));
                    articleWordsToSave.add(new ArticleWord(articleUrl, word, wordCount, tf));
                }

                dao.saveWords(wordsToSave);
                dao.saveArticleWords(articleWordsToSave);
            } else {
                System.out.println("article exists: " + articleUrl);
            }
        } else {
            System.out.println("Words list is empty for article: " + article);
        }
    }

    public static void main(String[] args) throws IOException, MyStemApplicationException {
        ArticleService articleService = new ArticleServiceImpl();
        String helloText = "привет как дела братишка привет";
        articleService.saveArticle(Utils.parseText(helloText), new Article("wow1", "asd", helloText));
    }
}
