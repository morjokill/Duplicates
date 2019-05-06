DROP DATABASE IF EXISTS duplicates;
CREATE DATABASE duplicates;

DROP USER IF EXISTS duplicates_user;
CREATE USER duplicates_user WITH PASSWORD 'duplicates';

GRANT ALL PRIVILEGES ON DATABASE duplicates TO duplicates_user;

DROP TABLE IF EXISTS clarification;
DROP TABLE IF EXISTS article_word;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS word;
DROP TABLE IF EXISTS library;

CREATE TABLE library(
  url VARCHAR(255) PRIMARY KEY,
  last_time_parsed TIMESTAMP,
  words_count BIGINT DEFAULT 0,
  before_range VARCHAR(255),
  after_range VARCHAR(255),
  last_parsed_in_range BIGINT DEFAULT 0
);

CREATE TABLE clarification (
  value VARCHAR(255),
  library VARCHAR(255) REFERENCES library (url),
  CONSTRAINT clarification_library_pk PRIMARY KEY (value, library)
)

CREATE TABLE article(
  url VARCHAR(255) PRIMARY KEY,
  library VARCHAR(255) REFERENCES library (url),
  text TEXT,
  words_count BIGINT
);

CREATE TABLE word(
  value VARCHAR(255),
  library VARCHAR(255) REFERENCES library (url),
  sum_count_in_collection BIGINT,
  articles_with_word_count INTEGER DEFAULT 1,
  CONSTRAINT word_pk PRIMARY KEY (value, library)
);

CREATE TABLE article_word(
  article VARCHAR(255),
  word VARCHAR(255),
  count INTEGER,
  tf DOUBLE PRECISION,
  weight DOUBLE PRECISION
);

ALTER TABLE article_word SET (FILLFACTOR = 70);

CREATE OR REPLACE FUNCTION recalculate_weight(in_library VARCHAR, in_count BIGINT)
  RETURNS INTEGER AS $$
DECLARE
  articles VARCHAR[];
  articles_count INTEGER;
BEGIN
  articles := ARRAY(SELECT url::VARCHAR FROM article a WHERE a.library = in_library);
  articles_count := array_upper(articles, 1);

  CREATE TEMP TABLE IF NOT EXISTS
    temp_table (word VARCHAR(255), article VARCHAR(255), weight DOUBLE PRECISION);
  TRUNCATE temp_table;

  INSERT INTO temp_table(word, article, weight)
    SELECT aw.word, aw.article, (ln(wd.articles_with_word_count::DOUBLE PRECISION / articles_count) * (-1) +
                                 ln(1 - exp(-1 * wd.sum_count_in_collection::DOUBLE PRECISION / in_count / articles_count))) *
                                aw.tf FROM article_word as aw
      INNER JOIN word wd on word = wd.value
    WHERE article = any (articles) AND wd.library = in_library;

  UPDATE article_word as aw SET weight = tt.weight FROM temp_table tt
  WHERE aw.word = tt.word AND aw.article = tt.article;

  RETURN articles_count;
END; $$
LANGUAGE 'plpgsql';