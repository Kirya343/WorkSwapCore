package org.workswap.core.datasource.admin.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.workswap.core.datasource.admin.model.enums.Status;
import org.workswap.core.datasource.admin.model.enums.TaskType;
import org.workswap.core.datasource.central.model.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class Task {

    public Task(String name,
                String description,
                LocalDateTime deadline,
                Status status,
                TaskType taskType,
                Long authorId) {
        this.name = name;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
        this.taskType = taskType;
        this.authorId = authorId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2000)
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime deadline;

    @Setter
    private LocalDateTime completed;

    @Setter
    @Enumerated(EnumType.STRING)
    private Status status;

    @Setter
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    @Setter
    @Transient
    private User executor;

    @Setter
    @Transient
    private User author;

    @Setter
    private Long executorId;

    private Long authorId;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskComment> comments = new ArrayList<>(); 

    @Transient // чтобы Hibernate не пытался сохранять это поле в БД
    public Duration getDuration() {
        if (createdAt != null && deadline != null) {
            return Duration.between(createdAt, deadline);
        }
        return null;
    }
}
