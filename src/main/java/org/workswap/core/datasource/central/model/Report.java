package org.workswap.core.datasource.central.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.workswap.core.datasource.central.model.ModelsSettings.ObjectType;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Report {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User author;

    @ManyToOne
    private User reportedUser;

    private Long reportedObjectId;

    @Enumerated(EnumType.STRING)
    private ObjectType reportedObjectType; // Например: "MESSAGE", "LISTING", "REVIEW"

    @Column(length = 2000)
    private String content;

    public enum ReportReason {
        SPAM,                    // Спам
        OFFENSIVE_LANGUAGE,      // Оскорбления / токсичное поведение
        INAPPROPRIATE_CONTENT,   // Неподобающий контент
        SCAM,                    // Мошенничество
        HARASSMENT,              // Преследование / домогательства
        HATE_SPEECH,             // Речь ненависти
        IMPERSONATION,           // Самозванство (выдаёт себя за другого)
        FALSE_INFORMATION,       // Ложная информация
        COPYRIGHT_VIOLATION,     // Нарушение авторских прав
        OTHER                    // Другая причина
    }

    @Enumerated(EnumType.STRING)
    private ReportReason reason;
    
    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ReportStatus {
        PENDING,     // Ожидает рассмотрения
        IN_REVIEW,   // В процессе
        RESOLVED,    // Решено
        REJECTED     // Отклонено
    }

    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(length = 2000)
    private String moderatorComment;

    private boolean anonymous = false;
}
