package ru.itis.duplicates.service;

import ru.itis.duplicates.model.ClarificationRange;

import java.util.List;

public interface LibraryService {
    void saveLibrary(String library, List<String> clarifications, ClarificationRange clarificationRange);
}
