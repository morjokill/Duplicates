package ru.itis.duplicates.service;

import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.Word;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;

import java.io.IOException;
import java.util.List;

public interface DuplicatesService {
    void saveArticle(List<String> words, Article article);
}
