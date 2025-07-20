package org.workswap.core.datasource.central.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.workswap.core.datasource.admin.model.Task;
import org.workswap.core.datasource.central.model.chat.Conversation;
import org.workswap.core.datasource.central.model.listingModels.Location;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"listings"})
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String sub; // Уникальный идентификатор от Google

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String bio;

    private String picture;
    private String avatarUrl;

    private boolean phoneVisible = true;  // Скрывать или отображать телефон
    private boolean emailVisible = true;  // Скрывать или отображать email

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_languages", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> languages = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Listing> listings = new ArrayList<>();

    @ManyToMany(mappedBy = "participants")
    private Set<Conversation> conversations = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean locked;
    private boolean enabled;

    private String avatarType; // "uploaded", "google", "default"

    private Double averageRating = 0.0; // Средний рейтинг пользователя
    private String phone;

    private Integer completedJobs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private boolean termsAccepted = false; // Приняты ли условия использования

    @Column(nullable = false)
    private LocalDateTime termsAcceptanceDate;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @Transient
    private List<Task> tasks;

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    // соц сети

    private boolean telegramConnected = false; // Подключен ли Telegram

    // Енумы 
    
    public enum Role {
        USER(1),
        PREMIUM(2),
        BUSINESS(3),
        ADMIN(4);

        private final int level;

        Role(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean isAtLeast(Role other) {
            return this.level >= other.level;
        }
    }
}
