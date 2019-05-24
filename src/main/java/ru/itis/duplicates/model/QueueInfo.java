package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class QueueInfo {
    private int queueSize;
    private String library;
    private List<String> clarifications;
    private LinkFinderStatus status;
    private long parsedLinks;
    private long allLinks;
    private ArticleUrlPattern articleUrlPattern;
    private AtomicLong lastParsed;

    public static QueueInfo getInstanceNoFinder(int queueSize, String library, List<String> clarifications, LinkFinderStatus status) {
        return new QueueInfo(queueSize, library, clarifications, status, 0, 0, null, null);
    }

    public static QueueInfo getEmpty() {
        return new QueueInfo(0, null, null, null, 0, 0, null, null);
    }
}
