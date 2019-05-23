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
    private String library;
    private List<String> clarifications;
    private LinkFinderStatus status;
    private long parsedLinks;
    private long allLinks;
    private ArticleUrlPattern articleUrlPattern;
    private AtomicLong lastParsed;

    public static QueueInfo getInstanceNoFinder(String library, List<String> clarifications, LinkFinderStatus status) {
        return new QueueInfo(library, clarifications, status, 0, 0, null, null);
    }
}
