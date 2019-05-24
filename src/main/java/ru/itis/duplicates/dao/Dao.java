package ru.itis.duplicates.dao;

import ru.itis.duplicates.model.*;

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

    void saveLibrary(String libraryUrl, String beforeRange, String afterRange);

    void updateLibrary(Library library);

    Boolean isArticleExists(String articleUrl);

    List<String> getArticlesFromLibrary(String libraryUrl);

    void vacuumWordArticleTable();

    Map<String, Long> mapArticlesWithSignatures(List<String> articles);

    Map<String, Word> getWords(List<String> words, String libraryUrl);

    Library getLibrary(String libraryUrl);

    void saveClarificationForLibrary(Clarification clarifications);

    List<String> getClarificationsForLibrary(String libraryUrl);

    List<Library> getIndexedLibraries();
}
