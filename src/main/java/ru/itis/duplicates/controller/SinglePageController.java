package ru.itis.duplicates.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.itis.duplicates.model.PutInQueue;
import ru.itis.duplicates.model.QueueInfo;
import ru.itis.duplicates.service.LibraryService;
import ru.itis.duplicates.service.impl.LibraryServiceImpl;

import java.util.List;

@Controller
@RequestMapping("/")
public class SinglePageController {
    private LibraryService libraryService;

    public SinglePageController() {
        this.libraryService = new LibraryServiceImpl();
    }

    @GetMapping("/hi")
    @ResponseBody
    public String hi() {
        return "русские вперед!";
    }

    @GetMapping("/queue")
    @ResponseBody
    public List<QueueInfo> getQueue() {
        return libraryService.getQueueInfo();
    }

    @PostMapping("/queue")
    @ResponseBody
    public List<QueueInfo> putInQueue(@RequestBody PutInQueue putInQueue) {
        return libraryService.addInQueue(putInQueue.getLibrary(), putInQueue.getClarifications());
    }
}
