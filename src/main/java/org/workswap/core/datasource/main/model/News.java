package org.workswap.core.datasource.main.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "news")
public class News {
    // Геттеры и сеттеры
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

    @Column
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime publishDate;

    @Column(nullable = false)
    private boolean published;

    @Transient
    private String localizedTitle;

    @Transient
    private String localizedExcerpt;

    @Transient
    private String localizedContent;
}