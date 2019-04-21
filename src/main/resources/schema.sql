DROP DATABASE IF EXISTS duplicates;
CREATE DATABASE duplicates;

DROP USER IF EXISTS duplicates_user;
CREATE USER duplicates_user WITH PASSWORD 'duplicates';

GRANT ALL PRIVILEGES ON DATABASE duplicates TO duplicates_user;

DROP TABLE IF EXISTS article_word;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS word;

CREATE TABLE article(
  id SERIAL PRIMARY KEY,
  url VARCHAR(255),
  text TEXT
);

CREATE TABLE word(
  id SERIAL PRIMARY KEY,
  value VARCHAR(255)
)

CREATE TABLE article_word(
  article_id INTEGER REFERENCES article (id),
  word_id INTEGER REFERENCES word (id),
  count INTEGER,
  weight DOUBLE PRECISION
)