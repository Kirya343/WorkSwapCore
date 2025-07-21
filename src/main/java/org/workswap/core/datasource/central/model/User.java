package org.workswap.core.datasource.central.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.workswap.core.datasource.admin.model.Task;
import org.workswap.core.datasource.central.model.chat.Conversation;
import org.workswap.core.datasource.central.model.enums.Role;
import org.workswap.core.datasource.central.model.listingModels.Location;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"listings"})
@Entity
@Table(name = "users")
public class User {

    public User(String name,
                String email,
                String sub,
                String avatarUrl,
                Role role,
                boolean termsAccepted) {
        this.name = name;
        this.email = email;
        this.sub = sub;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.termsAccepted = termsAccepted;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String sub; // Уникальный идентификатор от Google

    @Setter
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    private String bio;

    @Setter
    private String avatarUrl;

    @Setter
    private boolean phoneVisible = true;  // Скрывать или отображать телефон
    @Setter
    private boolean emailVisible = true;  // Скрывать или отображать email

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_languages", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> languages = new ArrayList<>();

    @Setter
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Listing> listings = new ArrayList<>();

    @ManyToMany(mappedBy = "participants")
    private Set<Conversation> conversations = new HashSet<>();

    @Setter
    @Enumerated(EnumType.STRING)
    private Role role;

    @Setter
    private boolean locked = false;

    @Setter
    private boolean enabled = true;

    @Setter
    private String avatarType; // "uploaded", "google", "default"

    @Setter
    private Double averageRating = 0.0; // Средний рейтинг пользователя

    @Setter
    private String phone;

    @Setter
    private Integer completedJobs;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean termsAccepted = false; // Приняты ли условия использования

    @Setter
    @Column(nullable = false)
    private LocalDateTime termsAcceptanceDate;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @Setter
    @Transient
    private List<Task> tasks;

    // соц сети

    @Setter
    private boolean telegramConnected = false; // Подключен ли Telegram
}
