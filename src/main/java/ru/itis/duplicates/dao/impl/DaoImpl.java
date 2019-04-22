package ru.itis.duplicates.dao.impl;

import com.sun.org.apache.xerces.internal.xs.StringList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.config.DaoConfig;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Word;

import javax.sql.DataSource;
import java.util.*;

public class DaoImpl implements Dao {
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    public static final String SQL_MAP_WORDS_WITH_ARTICLES = "SELECT * FROM article_word aw INNER JOIN word w on aw.word_id = w.id " +
            "WHERE w.value IN (:words);";

    public DaoImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public DaoImpl() {
        DataSource dataSource = DaoConfig.getDataSource();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Map<String, List<ArticleWord>> mapWordsWithArticles(List<String> words) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("words", words);

        return namedJdbcTemplate.query(SQL_MAP_WORDS_WITH_ARTICLES, params, getMapResultSetExtractor());
    }

    private static ResultSetExtractor<Map<String, List<ArticleWord>>> getMapResultSetExtractor() {
        return rs -> {
            Map<String, List<ArticleWord>> result = new HashMap<>();
            while (rs.next()) {
                String word = rs.getString("value");
                int wordId = rs.getInt("word_id");
                int articleId = rs.getInt("article_id");
                int count = rs.getInt("count");
                double weight = rs.getDouble("weight");

                //TODO: добавить?
                Article article = new Article(articleId, null, null);
                Word wordInstance = new Word(wordId, word);
                ArticleWord articleWord = new ArticleWord(wordInstance, article, count, weight);

                List<ArticleWord> articles;
                if (result.containsKey(word)) {
                    articles = result.get(word);
                    articles.add(articleWord);
                } else {
                    articles = new LinkedList<>();
                    articles.add(articleWord);
                    result.put(word, articles);
                }
            }
            return result;
        };
    }
}
