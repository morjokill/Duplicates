package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Library {
    private String url;
    private Timestamp lastTimeParsed;
    private long wordsCount;
    private String beforeRange;
    private String afterRange;
    private long lastParsedInRange;

    public Library(String url, Timestamp lastTimeParsed, long lastParsedInRange) {
        this.url = url;
        this.lastTimeParsed = lastTimeParsed;
        this.lastParsedInRange = lastParsedInRange;
    }
}
