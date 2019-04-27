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
    private int articlesWithWordCount;
    private double wordSumFreq;

    public Word(String value, double wordSumFreq) {
        this.value = value;
        this.wordSumFreq = wordSumFreq;
    }
}
