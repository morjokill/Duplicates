package ru.itis.duplicates.dao;

import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Word;

import java.util.List;

public interface Dao {
    int getArticlesCount();

    void saveArticle(Article article);

    void saveWords(List<Word> saveWords);

    void saveArticleWords(List<ArticleWord> saveArticleWords);

    void updateLibraryWordsCount(String library, long plusWords);

    void recalculateWeight();
}
