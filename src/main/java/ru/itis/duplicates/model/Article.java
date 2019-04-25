package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Article {
    private String url;
    private String library;
    private String text;
    private long wordsCount;

    public Article(String url, String library, String text) {
        this.url = url;
        this.library = library;
        this.text = text;
    }
}
