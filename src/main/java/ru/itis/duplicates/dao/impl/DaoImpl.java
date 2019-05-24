package ru.itis.duplicates.dao.impl;

import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.config.DaoConfig;
import ru.itis.duplicates.model.*;
import ru.itis.duplicates.util.Utils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static java.util.stream.Collectors.*;

public class DaoImpl implements Dao {
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedJdbcTemplate;

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
    private static final String SQL_SAVE_LIBRARY = "INSERT INTO library (url, before_range, after_range) " +
            "VALUES (?, ?, ?);";
    private static final String SQL_UPDATE_LIBRARY_LAST_PARSED = "UPDATE library SET last_time_parsed = ?, last_parsed_in_range = ? " +
            "WHERE url = ?;";
    private static final String SQL_SELECT_LIBRARY_EXISTS = "SELECT EXISTS(SELECT 1 FROM library WHERE url = ?);";
    private static final String SQL_SELECT_ARTICLE_EXISTS = "SELECT EXISTS(SELECT 1 FROM article WHERE url = ?);";
    private static final String SQL_ARTICLES_FROM_LIBRARY = "SELECT url FROM article WHERE library = ?;";
    private static final String SQL_VACUUM_WORD_ARTICLE_TABLE = "VACUUM FULL ANALYZE article_word;";
    private static final String SQL_FIND_MAX_WEIGHTED_WORDS_FOR_ARTICLES = "SELECT * FROM (" +
            "SELECT ROW_NUMBER() OVER (PARTITION BY article ORDER BY weight DESC) " +
            " AS r, t.article, t.word, t.weight " +
            "FROM article_word t) x " +
            "WHERE x.article IN (:articles) AND x.r <= 6;";
    private static final String SQL_MAP_WORDS_WITH_ARTICLES = "SELECT value, sum_count_in_collection," +
            "articles_with_word_count FROM word WHERE word.value IN (:words) AND library = :library;";
    private static final String SQL_SELECT_LIBRARY = "SELECT * FROM library WHERE url = ?;";
    private static final String SQL_SAVE_CLARIFICATION = "INSERT INTO clarification (value, library) " +
            " VALUES (?, ?);";
    private static final String SQL_SELECT_CLARIFICATIONS_FOR_LIBRARY = "SELECT c.value FROM clarification c WHERE library = ?;";
    private static final String SQL_SELECT_INDEXED_LIBRARIES = "SELECT * FROM library WHERE last_time_parsed NOTNULL;";
    private static final String SQL_UPDATE_ARTICLE_SIGNATURE = "UPDATE article SET signature = ? WHERE url = ?;";
    private static final String SQL_ARTICLES_WITH_SIGNS_FROM_LIBRARY = "SELECT url, signature FROM article WHERE library = ?;";
    private final SimpleJdbcCall recalculateWeightCall;

    private DaoImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        recalculateWeightCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("recalculate_weight")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("in_library", Types.VARCHAR),
                        new SqlParameter("in_count", Types.BIGINT),
                        new SqlOutParameter("recalculate_weight", Types.INTEGER));
    }

    public DaoImpl() {
        this(DaoConfig.getDataSource());
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
                ps.setLong(3, word.getCount());
                ps.setLong(4, word.getCount());
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
                ps.setLong(3, articleWord.getCount());
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

    @Override
    public void recalculateWeight(String libraryUrl, long wordsInLibrary) {
        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("in_library", libraryUrl)
                .addValue("in_count", wordsInLibrary);

        Map<String, Object> execute = recalculateWeightCall.execute(in);
        System.out.println(execute.get("recalculate_weight"));
    }

    @Override
    public Boolean isLibraryExists(String libraryUrl) {
        return jdbcTemplate.queryForObject(SQL_SELECT_LIBRARY_EXISTS, new Object[]{libraryUrl}, new int[]{Types.VARCHAR}, Boolean.class);
    }

    @Override
    public void saveLibrary(String libraryUrl, String beforeRange, String afterRange) {
        jdbcTemplate.update(SQL_SAVE_LIBRARY, new Object[]{libraryUrl, beforeRange, afterRange},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
    }

    @Override
    public void updateLibrary(Library library) {
        jdbcTemplate.update(SQL_UPDATE_LIBRARY_LAST_PARSED, new Object[]{library.getLastTimeParsed(), library.getLastParsedInRange(), library.getUrl()},
                new int[]{Types.TIMESTAMP, Types.BIGINT, Types.VARCHAR});
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

    @Override
    public void vacuumWordArticleTable() {
        jdbcTemplate.update(SQL_VACUUM_WORD_ARTICLE_TABLE);
    }

    @Override
    public void updateArticlesSignatures(List<String> articles) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("articles", articles);

        List<ArticleWord> articleWords = namedJdbcTemplate.query(SQL_FIND_MAX_WEIGHTED_WORDS_FOR_ARTICLES, params, new BeanPropertyRowMapper<>(ArticleWord.class));
        articleWords.sort(Comparator.comparing(ArticleWord::getWord));
        Map<String, String> collect = articleWords.stream().collect(groupingBy(ArticleWord::getArticle, mapping(ArticleWord::getWord, joining())));
        Map<String, Long> articleSignatureMap = new HashMap<>();
        for (String s : collect.keySet()) {
            articleSignatureMap.put(s, Utils.calculateCRC32(collect.get(s)));
        }
        Set<Map.Entry<String, Long>> entries = articleSignatureMap.entrySet();
        Iterator<Map.Entry<String, Long>> iterator = entries.iterator();
        jdbcTemplate.batchUpdate(SQL_UPDATE_ARTICLE_SIGNATURE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                if (iterator.hasNext()) {
                    Map.Entry<String, Long> next = iterator.next();
                    ps.setLong(1, next.getValue());
                    ps.setString(2, next.getKey());
                }
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });
    }

    @Override
    public Map<String, Word> getWords(List<String> words, String libraryUrl) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("words", words);
        params.addValue("library", libraryUrl);

        List<Word> wordsList = namedJdbcTemplate.query(SQL_MAP_WORDS_WITH_ARTICLES, params, new BeanPropertyRowMapper<>(Word.class));
        return wordsList.stream().collect(toMap(Word::getValue, e -> e));
    }

    @Override
    public Library getLibrary(String libraryUrl) {
        return jdbcTemplate.queryForObject(SQL_SELECT_LIBRARY, new Object[]{libraryUrl}, new BeanPropertyRowMapper<>(Library.class));
    }

    @Override
    public void saveClarificationForLibrary(Clarification clarification) {
        jdbcTemplate.update(SQL_SAVE_CLARIFICATION, new Object[]{clarification.getValue(), clarification.getLibrary()},
                new int[]{Types.VARCHAR, Types.VARCHAR});
    }

    @Override
    public List<String> getClarificationsForLibrary(String libraryUrl) {
        return jdbcTemplate.queryForList(SQL_SELECT_CLARIFICATIONS_FOR_LIBRARY, new Object[]{libraryUrl},
                new int[]{Types.VARCHAR}, String.class);
    }

    @Override
    public List<Library> getIndexedLibraries() {
        return jdbcTemplate.query(SQL_SELECT_INDEXED_LIBRARIES, new BeanPropertyRowMapper<>(Library.class));
    }

    @Override
    public List<Article> getArticlesWithSignatures(String libraryUrl) {
        return jdbcTemplate.query(SQL_ARTICLES_WITH_SIGNS_FROM_LIBRARY, new Object[]{libraryUrl},
                new int[]{Types.VARCHAR}, new BeanPropertyRowMapper<>(Article.class));
    }
}
