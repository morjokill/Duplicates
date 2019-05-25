package ru.itis.duplicates.service.impl;

import org.springframework.web.multipart.MultipartFile;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.*;
import ru.itis.duplicates.service.DuplicatesService;
import ru.itis.duplicates.util.Utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class DuplicatesServiceImpl implements DuplicatesService {
    private Dao dao;

    public DuplicatesServiceImpl() {
        this.dao = new DaoImpl();
    }

    public DuplicatesServiceImpl(Dao dao) {
        this.dao = dao;
    }

    //TODO: сделать 2 потока в бд, Future<>
    @Override
    public List<Duplicate> findDuplicates(String text, String libraryUrl) {
        LocalDateTime before = LocalDateTime.now();
        List<String> words;
        try {
            words = Utils.parseText(text);
        } catch (Exception e) {
            return Collections.emptyList();
        }

        if (words.size() == 0) {
            return Collections.emptyList();
        }

        List<Duplicate> doubles = new LinkedList<>();

        Library library = dao.getLibrary(libraryUrl);
        if (null != library) {
            List<Article> articlesFromLibrary = dao.getArticlesWithSignatures(libraryUrl);

            if (null == articlesFromLibrary || articlesFromLibrary.isEmpty()) {
                return Collections.emptyList();
            }

            List<ArticleWord> wordsFromDocument = new LinkedList<>();

            long libraryWordsCount = library.getWordsCount();

            int wordsCount = words.size();
            Map<String, Long> mapOfWordsCountForDocument = Utils.calculateWordsCount(words);
            double maxFreq = Utils.calculateMaxFrequencyInDocument(mapOfWordsCountForDocument, wordsCount);

            Map<String, Word> wordsFromLibrary = dao.getWords(words, libraryUrl);
            for (String word : mapOfWordsCountForDocument.keySet()) {
                Long wordCount = mapOfWordsCountForDocument.get(word);
                double freq = Utils.calculateFrequency(wordCount, wordsCount);
                double tf = Utils.calculateTf(freq, maxFreq);

                Word wordFromLibrary = wordsFromLibrary.get(word);

                int articlesWithWordCount = 0;
                long sumCountInCollection = 0;
                if (null != wordFromLibrary) {
                    articlesWithWordCount = wordFromLibrary.getArticlesWithWordCount();
                    sumCountInCollection = wordFromLibrary.getSumCountInCollection();
                }

                int articlesCount = articlesFromLibrary.size();

                double idf = Utils.calculateIdf(articlesWithWordCount, articlesCount);
                double sumFreq = (double) sumCountInCollection / libraryWordsCount;
                double pIdf = Utils.calculatePIdf(sumFreq, articlesCount);

                double rIdf = Utils.calculateRIdf(idf, pIdf);
                double weight = Utils.calculateWeight(tf, rIdf);

                wordsFromDocument.add(new ArticleWord(word, tf, weight));
            }
            wordsFromDocument.sort(Comparator.comparingDouble(ArticleWord::getWeight).reversed());
            List<ArticleWord> sortedMostWeightedWords = wordsFromDocument.stream().limit(6).collect(Collectors.toList());
            System.out.println(sortedMostWeightedWords);
            sortedMostWeightedWords.sort(Comparator.comparing(ArticleWord::getWord));
            StringBuilder articleStringBuilder = new StringBuilder();
            for (ArticleWord word : sortedMostWeightedWords) {
                articleStringBuilder.append(word.getWord());
            }
            String articleString = articleStringBuilder.toString();
            System.out.println(articleString);
            long articleSignature = Utils.calculateCRC32(articleString);
            System.out.println(articleSignature);

            for (Article article : articlesFromLibrary) {
                if (Long.compare(articleSignature, article.getSignature()) == 0) {
                    System.out.println("Duplicates with: " + article);
                    doubles.add(new Duplicate(article.getUrl()));
                }
            }
        }
        System.out.println("FIND DOUBLES TOOK: " + before.until(LocalDateTime.now(), ChronoUnit.MILLIS) + " ms");
        return doubles;
    }

    @Override
    public List<Duplicate> findDuplicates(MultipartFile file, String libraryUrl) {
        if (null != file && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();

            Extension extension = Extension.getExtension(originalFilename);
            if (extension == null) {
                return Collections.emptyList();
            }

            try {
                String text = extension.getText(file.getInputStream());
                return findDuplicates(text, libraryUrl);
            } catch (Exception e) {
                System.out.println("Could not parse file: " + originalFilename + " " + e.getMessage());
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
