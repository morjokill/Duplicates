package ru.itis.duplicates.dao;

import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Library;
import ru.itis.duplicates.model.Word;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface Dao {
    Integer getArticlesCount();

    void saveArticle(Article article);

    void saveWords(List<Word> saveWords);

    void saveArticleWords(List<ArticleWord> saveArticleWords);

    void updateLibraryWordsCount(String libraryUrl, long plusWords);

    void recalculateWeight(String libraryUrl, long wordsInLibrary);

    Boolean isLibraryExists(String libraryUrl);

    void saveLibrary(String libraryUrl);

    void updateLibraryLastTimeParsed(String libraryUrl, Timestamp lastTimeParsed);

    Boolean isArticleExists(String articleUrl);

    List<String> getArticlesFromLibrary(String libraryUrl);

    void vacuumWordArticleTable();

    Map<String, Long> mapArticlesWithSignatures(List<String> articles);

    Map<String, Word> getWords(List<String> words, String libraryUrl);

    Library getLibrary(String libraryUrl);
}
