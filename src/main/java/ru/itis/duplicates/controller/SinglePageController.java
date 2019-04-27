package ru.itis.duplicates.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class SinglePageController {

    @GetMapping("/hi")
    @ResponseBody
    public String hi() {
        return "русские вперед!";
    }
}
