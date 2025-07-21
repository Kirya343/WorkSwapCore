package org.workswap.core.datasource.central.model;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class NewsTranslation {

    public NewsTranslation(String language,
                           String title,
                           String shortDescription,
                           String description,
                           News news) {
        this.language = language;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.news = news;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String language;

    private String title;
    @Column(length = 1000)
    private String shortDescription;
    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "news_id")
    private News news;
}
