UPDATE article_word
SET weight = (log(sub.art_count::DOUBLE PRECISION / (SELECT count(*) FROM article WHERE library = sub.library)) * -1
              + (log(1 - exp(-1 * (SELECT sum(count) FROM article_word INNER JOIN article a on article_word.article = a.url
  INNER JOIN library l on a.library = l.url WHERE article_word.word = sub.word AND l.url = sub.library)::DOUBLE PRECISION
                                                                                                                / (SELECT count(*) FROM article WHERE library = sub.library))))) * sub.tf FROM
(SELECT word, count, tf, articles_with_word_count as art_count, library FROM article_word
INNER JOIN word w2 on article_word.word = w2.value INNER JOIN article a on article_word.article = a.url) as sub
WHERE sub.word = article_word.word;

CREATE TEMP TABLE tempTable (id BIGINT NOT NULL, field(s) to be updated,
CONSTRAINT tempTable_pkey PRIMARY KEY (id));

VACUUM FULL article_word;

SELECT relname, n_dead_tup FROM pg_stat_user_tables;

SELECT
  relname AS "table_name",
  pg_size_pretty(pg_table_size(C.oid)) AS "table_size"
FROM
  pg_class C
  LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace)
WHERE nspname NOT IN ('pg_catalog', 'information_schema') AND nspname !~ '^pg_toast' AND relkind IN ('r')
ORDER BY pg_table_size(C.oid)
DESC LIMIT 1;;

SELECT relname, last_vacuum, last_autovacuum FROM pg_stat_user_tables;

SELECT name, setting FROM pg_settings WHERE name='autovacuum';

SELECT reloptions FROM pg_class WHERE relname='article_word';


SELECT relname, reloptions FROM pg_class;

TRUNCATE article_word;

select * from pg_stat_activity;

SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'duplicates' -- ← change this to your DB
      AND pid <> pg_backend_pid();

