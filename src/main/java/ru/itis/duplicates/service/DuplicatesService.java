package ru.itis.duplicates.service;

import ru.itis.duplicates.model.Duplicate;

import java.util.List;

public interface DuplicatesService {
    List<Duplicate> findDuplicates(String text, String libraryUrl);
}
