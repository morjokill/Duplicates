package ru.itis.duplicates.service;

import ru.itis.duplicates.model.Article;

import java.util.List;

public interface ArticleService {
    void saveArticle(List<String> words, Article article);
}
