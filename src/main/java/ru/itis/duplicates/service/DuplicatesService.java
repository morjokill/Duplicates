package ru.itis.duplicates.service;

import java.util.List;

public interface DuplicatesService {
    List<String> findDuplicates(List<String> words, String libraryUrl);
}
