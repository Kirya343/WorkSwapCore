package org.workswap.core.datasource.central.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "news")
public class News {
    
    public News(List<String> communities,
                String imageUrl,
                User author) {
        this.communities = communities;
        this.author = author;
        this.imageUrl = imageUrl;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKey(name = "language") // ключ — это язык (например, "ru")
    private Map<String, NewsTranslation> translations = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "news_communities", joinColumns = @JoinColumn(name = "news_id"))
    @Column(name = "language")
    private List<String> communities = new ArrayList<>();

    @Setter
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @CreationTimestamp
    private LocalDateTime publishDate;

    private boolean published = true;

    @Setter
    @Transient
    private String localizedTitle;

    @Setter
    @Transient
    private String localizedExcerpt;

    @Setter
    @Transient
    private String localizedContent;
}