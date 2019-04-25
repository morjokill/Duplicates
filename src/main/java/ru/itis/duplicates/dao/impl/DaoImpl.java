package ru.itis.duplicates.dao.impl;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.config.DaoConfig;
import ru.itis.duplicates.model.Article;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Word;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class DaoImpl implements Dao {
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    private static final String SQL_MAP_WORDS_WITH_ARTICLES = "SELECT * FROM article_word WHERE word IN (:words);";
    private static final String SQL_ARTICLES_COUNT = "SELECT count(*) FROM article;";
    private static final String SQL_SAVE_ARTICLE = "INSERT INTO article (url, library, text, words_count)" +
            " VALUES (?, ?, ?, ?);";
    private static final String SQL_SAVE_WORD = "INSERT INTO word (value, word_sum_freq) " +
            "VALUES (?, ?) ON CONFLICT (VALUE) DO UPDATE " +
            "SET articles_with_word_count = word.articles_with_word_count + 1," +
            "  word_sum_freq = word.word_sum_freq + ?;";
    private static final String SQL_SAVE_ARTICLE_WORD = "INSERT INTO article_word (article, word, freq, tf) " +
            "VALUES (?, ?, ?, ?);";

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
    public int getArticlesCount() {
        return jdbcTemplate.queryForObject(SQL_ARTICLES_COUNT, Integer.class);
    }

    @Override
    public void saveArticle(Article article) {
        jdbcTemplate.update(SQL_SAVE_ARTICLE, new Object[]{article.getUrl(), article.getLibrary(),
                        article.getText(), article.getWordsCount()},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BIGINT});
    }

    @Override
    public void saveWords(List<Word> saveWords) {
        jdbcTemplate.batchUpdate(SQL_SAVE_WORD, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Word word = saveWords.get(i);
                ps.setString(1, word.getValue());
                ps.setDouble(2, word.getWordSumFreq());
                ps.setDouble(3, word.getWordSumFreq());
            }

            @Override
            public int getBatchSize() {
                return saveWords.size();
            }
        });
    }

    @Override
    public void saveArticleWords(List<ArticleWord> saveArticleWords) {
        jdbcTemplate.batchUpdate(SQL_SAVE_ARTICLE_WORD, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ArticleWord articleWord = saveArticleWords.get(i);
                ps.setString(1, articleWord.getArticle());
                ps.setString(2, articleWord.getWord());
                ps.setDouble(3, articleWord.getFreq());
                ps.setDouble(4, articleWord.getTf());
            }

            @Override
            public int getBatchSize() {
                return saveArticleWords.size();
            }
        });
    }
}
