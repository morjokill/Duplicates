DROP DATABASE IF EXISTS duplicates;
CREATE DATABASE duplicates;

DROP USER IF EXISTS duplicates_user;
CREATE USER duplicates_user WITH PASSWORD 'duplicates';

GRANT ALL PRIVILEGES ON DATABASE duplicates TO duplicates_user;

DROP TABLE IF EXISTS article_word;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS word;
DROP TABLE IF EXISTS library;

CREATE TABLE library(
  url VARCHAR(255) PRIMARY KEY,
  last_time_parsed TIMESTAMP,
  words_count BIGINT DEFAULT 0
);

CREATE TABLE article(
  url VARCHAR(255) PRIMARY KEY,
  library VARCHAR(255) REFERENCES library (url),
  text TEXT,
  words_count BIGINT
);

CREATE TABLE word(
  value VARCHAR(255) PRIMARY KEY,
  articles_with_word_count INTEGER DEFAULT 1
);

CREATE TABLE article_word(
  article VARCHAR(255),
  word VARCHAR(255),
  count INTEGER,
  tf DOUBLE PRECISION,
  weight DOUBLE PRECISION
);