package org.workswap.core.datasource.central.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.workswap.core.datasource.central.model.enums.ObjectType;
import org.workswap.core.datasource.central.model.enums.ReportReason;
import org.workswap.core.datasource.central.model.enums.ReportStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class Report {

    public Report(User author,
                  User reportedUser,
                  Long reportedObjectId,
                  ObjectType reportedObjectType,
                  String content,
                  ReportReason reason,
                  boolean anonymous) {
        this.author = author;
        this.reportedUser = reportedUser;
        this.reportedObjectId = reportedObjectId;
        this.reportedObjectType = reportedObjectType;
        this.content = content;
        this.reason = reason;
        this.anonymous = anonymous;
    }
    
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

    @Enumerated(EnumType.STRING)
    private ReportReason reason;
    
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Setter
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;

    @Setter
    @Column(length = 2000)
    private String moderatorComment;

    private boolean anonymous = false;
}
