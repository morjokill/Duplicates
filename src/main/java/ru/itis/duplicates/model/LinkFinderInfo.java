package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.itis.duplicates.task.LinksFinder;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LinkFinderInfo {
    private LinksFinder finder;
    private String library;
    private List<String> clarifications;
    private LinkFinderStatus status;
    private String uuid;

    public static LinkFinderInfo getNewInstance(LinksFinder finder, String library, List<String> clarifications, String uuid) {
        return new LinkFinderInfo(finder, library, clarifications, LinkFinderStatus.NEW, uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkFinderInfo that = (LinkFinderInfo) o;
        return Objects.equals(library, that.library);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), library);
    }
}
