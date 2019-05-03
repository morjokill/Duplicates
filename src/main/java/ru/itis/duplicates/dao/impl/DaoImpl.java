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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

public class DaoImpl implements Dao {
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    private static final String SQL_MAP_WORDS_WITH_ARTICLES = "SELECT * FROM article_word WHERE word IN (:words);";
    private static final String SQL_ARTICLES_COUNT = "SELECT count(*) FROM article;";
    private static final String SQL_SAVE_ARTICLE = "INSERT INTO article (url, library, text, words_count)" +
            " VALUES (?, ?, ?, ?);";
    private static final String SQL_SAVE_WORD = "INSERT INTO word (value, library, sum_count_in_collection) " +
            "VALUES (?, ?, ?) ON CONFLICT ON CONSTRAINT word_pk DO " +
            "UPDATE SET articles_with_word_count = word.articles_with_word_count + 1, " +
            "sum_count_in_collection = word.sum_count_in_collection + ?;";
    private static final String SQL_SAVE_ARTICLE_WORD = "INSERT INTO article_word (article, word, count, tf) " +
            "VALUES (?, ?, ?, ?);";
    private static final String SQL_UPDATE_LIBRARY_WORDS_COUNT = "UPDATE library SET words_count = words_count + ?" +
            " WHERE library.url = ?;";
    //TODO: добавить условие library
    private static final String SQL_RECALCULATE_WEIGHT = "UPDATE article_word " +
            "SET weight = (log(sub.art_count::DOUBLE PRECISION / (SELECT count(*) FROM article WHERE library = sub.library)) * -1 " +
            "+ (log(1 - exp(-1 * (SELECT sum(count) FROM article_word INNER JOIN article a on article_word.article = a.url " +
            "INNER JOIN library l on a.library = l.url WHERE article_word.word = sub.word AND l.url = sub.library)::DOUBLE PRECISION " +
            "/ (SELECT count(*) FROM article WHERE library = sub.library))))) * sub.tf FROM " +
            "(SELECT word, count, tf, articles_with_word_count as art_count, library FROM article_word " +
            "INNER JOIN word w2 on article_word.word = w2.value INNER JOIN article a on article_word.article = a.url) as sub " +
            "WHERE sub.word = article_word.word;";
    private static final String SQL_SAVE_LIBRARY = "INSERT INTO library (url) " +
            "VALUES (?);";
    private static final String SQL_UPDATE_LIBRARY_LAST_PARSED = "UPDATE library SET last_time_parsed = ? " +
            "WHERE url = ?;";
    private static final String SQL_SELECT_LIBRARY_EXISTS = "SELECT EXISTS(SELECT 1 FROM library WHERE url = ?);";
    private static final String SQL_SELECT_ARTICLE_EXISTS = "SELECT EXISTS(SELECT 1 FROM article WHERE url = ?);";
    private static final String SQL_ARTICLES_FROM_LIBRARY = "SELECT url FROM article WHERE library = ?;";

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
    public Integer getArticlesCount() {
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
                ps.setString(2, word.getLibraryUrl());
                ps.setInt(3, word.getCount());
                ps.setInt(4, word.getCount());
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
                ps.setInt(3, articleWord.getCount());
                ps.setDouble(4, articleWord.getTf());
            }

            @Override
            public int getBatchSize() {
                return saveArticleWords.size();
            }
        });
    }

    @Override
    public void updateLibraryWordsCount(String libraryUrl, long plusWords) {
        jdbcTemplate.update(SQL_UPDATE_LIBRARY_WORDS_COUNT, new Object[]{plusWords, libraryUrl},
                new int[]{Types.BIGINT, Types.VARCHAR});
    }

    //TODO: сделать через инсерт в темп таблицу
    /*https://stackoverflow.com/questions/3361291/slow-simple-update-query-on-postgresql-database-with-3-million-rows*/
    /*https://stackoverflow.com/questions/3100072/postgresql-slow-on-a-large-table-with-arrays-and-lots-of-updates/3100232#3100232*/
    /*https://web.archive.org/web/20120229084713/http://pgsql.tapoueh.org/site/html/misc/hot.html*/
    /*Today I've spent many hours with similar issue. I've found a solution: drop all the constraints/indices before
    the update. No matter whether the column being updated is indexed or not, it seems like psql updates all
    the indices for all the updated rows. After the update is finished, add the constraints/indices back.*/
    /*CREATE TEMP TABLE tempTable (id BIGINT NOT NULL, field(s) to be updated,
CONSTRAINT tempTable_pkey PRIMARY KEY (id));*/
    @Override
    public void recalculateWeight() {
        jdbcTemplate.update(SQL_RECALCULATE_WEIGHT);
    }

    @Override
    public Boolean isLibraryExists(String libraryUrl) {
        return jdbcTemplate.queryForObject(SQL_SELECT_LIBRARY_EXISTS, new Object[]{libraryUrl}, new int[]{Types.VARCHAR}, Boolean.class);
    }

    @Override
    public void saveLibrary(String libraryUrl) {
        jdbcTemplate.update(SQL_SAVE_LIBRARY, new Object[]{libraryUrl},
                new int[]{Types.VARCHAR});
    }

    @Override
    public void updateLibraryLastTimeParsed(String libraryUrl, Timestamp lastTimeParsed) {
        jdbcTemplate.update(SQL_UPDATE_LIBRARY_LAST_PARSED, new Object[]{lastTimeParsed, libraryUrl},
                new int[]{Types.TIMESTAMP, Types.VARCHAR});
    }

    @Override
    public Boolean isArticleExists(String articleUrl) {
        return jdbcTemplate.queryForObject(SQL_SELECT_ARTICLE_EXISTS, new Object[]{articleUrl}, new int[]{Types.VARCHAR}, Boolean.class);
    }

    @Override
    public List<String> getArticlesFromLibrary(String libraryUrl) {
        return jdbcTemplate.queryForList(SQL_ARTICLES_FROM_LIBRARY, new Object[]{libraryUrl},
                new int[]{Types.VARCHAR}, String.class);
    }

    public static void main(String[] args) {
        Dao da = new DaoImpl();
        System.out.println(da.getArticlesFromLibrary("https://habr.com"));
    }
}
