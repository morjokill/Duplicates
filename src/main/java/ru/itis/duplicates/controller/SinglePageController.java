package ru.itis.duplicates.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itis.duplicates.model.*;
import ru.itis.duplicates.service.DuplicatesService;
import ru.itis.duplicates.service.LibraryService;
import ru.itis.duplicates.service.impl.DuplicatesServiceImpl;
import ru.itis.duplicates.service.impl.LibraryServiceImpl;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/")
public class SinglePageController {
    private LibraryService libraryService;
    private DuplicatesService duplicatesService;

    public SinglePageController() {
        this.libraryService = new LibraryServiceImpl();
        this.duplicatesService = new DuplicatesServiceImpl();
    }

    @GetMapping("/hi")
    @ResponseBody
    public String hi() {
        return "русские вперед!";
    }

    @GetMapping("/queue")
    @ResponseBody
    public QueueInfo getQueue() {
        return libraryService.getQueueInfo();
    }

    @PostMapping("/queue")
    @ResponseBody
    public String putInQueue(@RequestBody PutInQueue putInQueue) {
        return libraryService.addInQueue(putInQueue.getLibrary(), putInQueue.getClarifications());
    }

    @PostMapping("/remove")
    @ResponseBody
    public boolean deleteFromQueue(@RequestBody String uuid) {
        return libraryService.removeFromQueue(uuid);
    }

    @GetMapping("/libs")
    @ResponseBody
    public List<Library> getIndexedLibraries() {
        return libraryService.getIndexedLibraries();
    }

    @PostMapping("/check")
    @ResponseBody
    public List<Duplicate> checkDocument(@RequestBody CheckInstance checkInstance) {
        return duplicatesService.findDuplicates(checkInstance.getText(), checkInstance.getLibrary());
    }

    @PostMapping(value = "/checkFile")
    @ResponseBody
    public List<Duplicate> checkFile(@RequestParam("file") MultipartFile file,
                                     @RequestParam("library") String library) {
        return duplicatesService.findDuplicates(file, library);
    }
}
