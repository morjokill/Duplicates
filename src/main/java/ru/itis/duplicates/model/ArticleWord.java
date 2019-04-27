package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class ArticleWord {
    private String article;
    private String word;
    private int count;
    private double tf;
    private double weight;

    public ArticleWord(String article, String word, int count, double tf) {
        this.article = article;
        this.word = word;
        this.count = count;
        this.tf = tf;
    }
}
