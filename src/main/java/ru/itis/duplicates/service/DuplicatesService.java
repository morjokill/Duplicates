package ru.itis.duplicates.service;

import ru.itis.duplicates.model.ArticleDuplicate;

import java.util.List;

public interface DuplicatesService {
    List<ArticleDuplicate> findDuplicates(List<String> words);
}
