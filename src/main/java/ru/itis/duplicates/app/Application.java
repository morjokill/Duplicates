package ru.itis.duplicates.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.stachek66.nlp.mystem.holding.Factory;
import ru.stachek66.nlp.mystem.holding.MyStem;
import scala.Option;

@SpringBootApplication(scanBasePackages = "ru.itis.duplicates")
public class Application {
    private static MyStem myStemAnalyzer;

    public static void main(String[] args) {
        initMyStem();

        SpringApplication.run(Application.class, args);
    }

    private static void initMyStem() {
        myStemAnalyzer = new Factory("-igd --format json")
                .newMyStem("3.0", Option.empty()).get();
    }

    public static MyStem getMyStemAnalyzer() {
        if (null == myStemAnalyzer) {
            initMyStem();
        }
        return myStemAnalyzer;
    }
}
