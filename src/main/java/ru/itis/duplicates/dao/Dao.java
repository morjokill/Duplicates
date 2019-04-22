package ru.itis.duplicates.dao;

import ru.itis.duplicates.model.ArticleWord;

import java.util.List;
import java.util.Map;

public interface Dao {
    Map<String, List<ArticleWord>> mapWordsWithArticles(List<String> words);
}
