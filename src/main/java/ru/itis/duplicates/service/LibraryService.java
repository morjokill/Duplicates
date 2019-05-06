package ru.itis.duplicates.service;

import ru.itis.duplicates.model.LinkFinderInfo;

import java.util.List;
import java.util.Queue;

public interface LibraryService {
    void saveLibrary(LinkFinderInfo linkFinderInfo);

    Queue<LinkFinderInfo> addInQueue(String library, List<String> clarifications);

    Queue<LinkFinderInfo> getQueue();

    Queue<LinkFinderInfo> removeFromQueue(String library);
}
