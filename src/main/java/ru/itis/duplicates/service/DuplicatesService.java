package ru.itis.duplicates.service;

import org.springframework.web.multipart.MultipartFile;
import ru.itis.duplicates.model.Duplicate;

import java.util.List;

public interface DuplicatesService {
    List<Duplicate> findDuplicates(String text, String libraryUrl);

    List<Duplicate> findDuplicates(MultipartFile file, String libraryUrl);
}
