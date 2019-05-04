package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Word {
    private String value;
    private Long count;
    private int articlesWithWordCount;
    private long sumCountInCollection;
    private String libraryUrl;

    public Word(String value, Long count, String libraryUrl) {
        this.value = value;
        this.count = count;
        this.libraryUrl = libraryUrl;
    }

    public Word(String value, int articlesWithWordCount, long sumCountInCollection) {
        this.value = value;
        this.articlesWithWordCount = articlesWithWordCount;
        this.sumCountInCollection = sumCountInCollection;
    }
}
