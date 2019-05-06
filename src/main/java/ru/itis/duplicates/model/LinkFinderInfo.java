package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itis.duplicates.task.LinksFinder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LinkFinderInfo {
    private LinksFinder finder;
    private String library;
    private List<String> clarifications;
    private LinkFinderStatus status;

    public static LinkFinderInfo getNewInstance(LinksFinder finder, String library, List<String> clarifications) {
        return new LinkFinderInfo(finder, library, clarifications, LinkFinderStatus.NEW);
    }
}
