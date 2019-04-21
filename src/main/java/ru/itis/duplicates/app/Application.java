package ru.itis.duplicates.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ru.itis.duplicates")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
