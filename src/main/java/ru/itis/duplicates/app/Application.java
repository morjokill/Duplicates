package ru.itis.duplicates.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.itis.duplicates.util.Utils;
import ru.stachek66.nlp.mystem.holding.Factory;
import ru.stachek66.nlp.mystem.holding.MyStem;
import scala.Option;

import java.io.IOException;
import java.util.Set;

@SpringBootApplication(scanBasePackages = "ru.itis.duplicates")
public class Application {
    private final static String stopWordsFilePath = "src\\main\\resources\\stop_words.txt";
    private static Set<String> stopWords;
    private static MyStem myStemAnalyzer;

    //TODO: регать jdbc в начале?
    public static void main(String[] args) throws IOException {
        initMyStem();
        initStopWords();

        SpringApplication.run(Application.class, args);
    }

    public static MyStem getMyStemAnalyzer() {
        if (null == myStemAnalyzer) {
            initMyStem();
        }
        return myStemAnalyzer;
    }

    private static void initMyStem() {
        myStemAnalyzer = new Factory("-igd --format json")
                .newMyStem("3.0", Option.empty()).get();
    }

    //TODO: только 1 раз, уебок
    public static Set<String> getStopWords() throws IOException {
        if (null == stopWords) {
            initStopWords();
        }
        return stopWords;
    }

    private static void initStopWords() throws IOException {
        stopWords = Utils.readFile(stopWordsFilePath);
    }
}
