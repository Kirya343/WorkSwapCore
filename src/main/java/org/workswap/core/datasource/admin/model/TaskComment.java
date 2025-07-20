package org.workswap.core.datasource.admin.model;

import org.workswap.core.datasource.central.model.User;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TaskComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    private Long authorId;

    @Transient
    private User author;

    @ManyToOne
    private Task task;
}
