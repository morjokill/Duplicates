package ru.itis.duplicates.service;

import ru.itis.duplicates.model.Library;
import ru.itis.duplicates.model.LinkFinderInfo;
import ru.itis.duplicates.model.QueueInfo;

import java.util.List;
import java.util.Queue;

public interface LibraryService {
    void saveLibrary(LinkFinderInfo linkFinderInfo);

    String addInQueue(String library, List<String> clarifications);

    Queue<LinkFinderInfo> getQueue();

    QueueInfo getQueueInfo();

    boolean removeFromQueue(String uuid);

    List<Library> getIndexedLibraries();
}
