package org.workswap.core.datasource.central.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
public class Resume {

    public Resume(User user) {
        this.user = user;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Setter
    private String profession;
    @Setter
    private Double experience;
    @Setter
    private String education;
    @Setter
    private String skills;
    @Setter
    private String about;
    @Setter
    private String contacts;
    @Setter
    private String filePath;
    @Setter
    private boolean published; // Новое поле для статуса публикации

    @Setter
    @ElementCollection
    @CollectionTable(name = "resume_languages", joinColumns = @JoinColumn(name = "resume_id"))
    @MapKeyColumn(name = "language")
    @Column(name = "level")
    private Map<String, String> languages = new HashMap<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Setter
    @Transient
    private List<String> languagesForm;

    @Setter
    @Transient
    private List<String> languageLevelsForm;

    @Setter
    @Transient
    private Map<String, String> languagesFormMap = new HashMap<>();
}