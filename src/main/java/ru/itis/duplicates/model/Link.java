package ru.itis.duplicates.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Link {
    private String url;
    private LinkStatus status;
    private Document html;

    private Link(String url, LinkStatus status) {
        this.url = url;
        this.status = status;
    }

    public static Link getNewLink(String url) {
        return new Link(url, LinkStatus.NEW);
    }

    @Override
    public String toString() {
        return "Link{" +
                "url='" + url + '\'' +
                ", status=" + status +
                '}';
    }
}
