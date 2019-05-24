package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class IndexedLibrary {
    private String url;
    private Timestamp lastTimeParsed;
    private long wordsCount;
    private String beforeRange;
    private String afterRange;
    private long lastParsedInRange;
    private long articlesCount;
    private List<String> clarifications;
}
