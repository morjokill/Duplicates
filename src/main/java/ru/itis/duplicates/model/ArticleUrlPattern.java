package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class ArticleUrlPattern {
    private boolean hasPattern;
    private String beforeRange;
    private String afterRange;

    public ArticleUrlPattern(String beforeRange, String afterRange) {
        this.hasPattern = true;
        this.beforeRange = beforeRange;
        this.afterRange = afterRange;
    }

    private ArticleUrlPattern(boolean hasPattern) {
        this.hasPattern = hasPattern;
    }

    public static ArticleUrlPattern getNoPatternInstance() {
        return new ArticleUrlPattern(false);
    }
}
