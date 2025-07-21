package org.workswap.core.datasource.central.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.workswap.core.datasource.central.model.User;

@Getter
@Entity
@NoArgsConstructor
public class Message {

    public Message(Conversation conversation,
                   User sender,
                   User receiver,
                   String text) {
        this.conversation = conversation;
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)  // Или убедитесь, что всегда работаете в транзакции
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(columnDefinition = "TEXT")
    private String text;

    @CreationTimestamp
    private LocalDateTime sentAt;

    @Setter
    @Column(name = "is_read")
    private boolean read = false;

    @Transient
    private boolean isOwn;

    public boolean isOwn(User user) {
        return sender != null && sender.equals(user);
    }

    public void setIsOwn(boolean isOwn) {
        this.isOwn = isOwn;
    }

}
