package ru.itis.duplicates.service.impl;

import lombok.extern.java.Log;
import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.ClarificationRange;
import ru.itis.duplicates.service.ArticleService;
import ru.itis.duplicates.service.LibraryService;
import ru.itis.duplicates.task.LinksFinder;
import ru.itis.duplicates.util.Utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Log
public class LibraryServiceImpl implements LibraryService {
    private ArticleService articleService;
    private Dao dao;

    public LibraryServiceImpl() {
        this.articleService = new ArticleServiceImpl();
        this.dao = new DaoImpl();
    }

    public LibraryServiceImpl(ArticleService articleService, Dao dao) {
        this.articleService = articleService;
        this.dao = dao;
    }

    //TODO: при добавлении не надо жестко смотреть по названию. 1 и тот же ресурс может быть как url так и файлами
    @Override
    public void saveLibrary(String library, List<String> clarifications, ClarificationRange clarificationRange) {
        try {
            String rootUrl = Utils.getRootUrl(library);

            //TODO: либо так либо так. лучше фабрика или енум
            LinksFinder lf;
            List<String> parsedClarifications = parseClarifications(clarifications);
            if (!dao.isLibraryExists(rootUrl)) {
                dao.saveLibrary(rootUrl);

                lf = new LinksFinder(library, rootUrl, parsedClarifications, null);
            } else {
                List<String> parsedLinks = dao.getArticlesFromLibrary(rootUrl);

                lf = new LinksFinder(library, rootUrl, parsedClarifications, parsedLinks);

                System.out.println("exists: " + rootUrl);
            }

            lf.getAllLinksFromSite();

            //TODO: вынести
            LocalDateTime currentTime = LocalDateTime.now();
            dao.updateLibraryLastTimeParsed(rootUrl, Timestamp.valueOf(currentTime));
            //TODO: РАБОТАЕТ, НО ПИЗДЕЦ ДОЛГО
            //TODO: выполнять только 1 раз надо, а не для каждой статьи,
            // ибо лок и долго, либо же пофискить, чтобы быстрее было
            dao.recalculateWeight(rootUrl);

        } catch (Exception e) {
            System.out.println("links finder gg: " + e);
        }
        //TODO: сделать норм return
    }

    private static List<String> parseClarifications(List<String> clarifications) {
        List<String> parsedClarifications = new LinkedList<>();

        for (String clarification : clarifications) {
            if (Utils.isClarification(clarification)) {
                parsedClarifications.add(Utils.formatClarification(clarification));
            }
        }
        return parsedClarifications;
    }

    public static void main(String[] args) {
        LibraryService libraryService = new LibraryServiceImpl();
        libraryService.saveLibrary("https://pikabu.ru/", Arrays.asList("story"), null);
    }
}
