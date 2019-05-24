package ru.itis.duplicates.service;

import ru.itis.duplicates.model.IndexedLibrary;
import ru.itis.duplicates.model.LinkFinderInfo;
import ru.itis.duplicates.model.QueueInfo;

import java.util.List;
import java.util.Queue;

public interface LibraryService {
    void saveLibrary(LinkFinderInfo linkFinderInfo);

    QueueInfo addInQueue(String library, List<String> clarifications);

    Queue<LinkFinderInfo> getQueue();

    QueueInfo getQueueInfo();

    Queue<LinkFinderInfo> removeFromQueue(String library);

    List<IndexedLibrary> getIndexedLibraries();
}
