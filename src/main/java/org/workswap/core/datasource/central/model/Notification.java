package org.workswap.core.datasource.central.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.workswap.core.datasource.central.model.enums.Importance;
import org.workswap.core.datasource.central.model.enums.NotificationType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
public class Notification {

    public Notification(User recipient,
                        String title,
                        String content,
                        String link,
                        NotificationType type,
                        Importance importance) {
        this.recipient = recipient;
        this.title = title;
        this.content = content;
        this.link = link;
        this.type = type;
        this.importance = importance;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name ="recipient_id")
    private User recipient;

    @Setter
    private boolean isRead = false;
    
    private String title;

    @Column(length = 2000)
    private String content;

    private String link;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private Importance importance;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
