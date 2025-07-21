package org.workswap.core.datasource.central.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.workswap.core.datasource.central.model.Listing;
import org.workswap.core.datasource.central.model.User;

@Getter
@Entity
@NoArgsConstructor
public class Conversation {

    public Conversation(Set<User> participants,
                        Listing listing) {
        this.participants = participants;
        this.listing = listing;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "conversation_participants",
        joinColumns = @JoinColumn(name = "conversation_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    @Setter
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Message> messages = new ArrayList<>();

    @ManyToOne
    @Setter
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Transient
    private long unreadCount;

    @Transient
    private String lastMessagePreview;

    @Transient
    private String lastMessageTime;

    @Transient
    private transient User interlocutor; // Временное поле для хранения собеседника

    public User getInterlocutor(User currentUser) {
        return participants.stream()
                .filter(user -> !user.equals(currentUser))
                .findFirst()
                .orElse(null);
    }

    // Получить последнее сообщение
    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
}

