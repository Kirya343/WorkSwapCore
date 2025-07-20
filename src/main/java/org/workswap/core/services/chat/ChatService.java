package org.workswap.core.services.chat;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.workswap.core.datasource.main.model.Listing;
import org.workswap.core.datasource.main.model.User;
import org.workswap.core.datasource.main.model.DTOs.ConversationDTO;
import org.workswap.core.datasource.main.model.chat.Conversation;
import org.workswap.core.datasource.main.model.chat.Message;
import org.workswap.core.datasource.main.repository.ConversationRepository;
import org.workswap.core.datasource.main.repository.MessageRepository;
import org.workswap.core.services.ListingService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ListingService listingService;

    public Conversation getOrCreateConversation(Set<User> participants, Listing listing) {
        if (participants.size() != 2) {
            throw new IllegalArgumentException("Conversation must have exactly 2 participants.");
        }

        Iterator<User> iterator = participants.iterator();
        User user1 = iterator.next();
        User user2 = iterator.next();

        // Сначала ищем чат с привязкой к объявлению
        if (listing != null) {
            Optional<Conversation> existing = conversationRepository
                    .findBetweenUsersAndListing(user1, user2, listing);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // Ищем общий чат без привязки к объявлению
        Optional<Conversation> existing = conversationRepository
                .findBetweenUsers(user1, user2);
        if (existing.isPresent() && listing == null) {
            return existing.get();
        }

        // Создаём новый
        Conversation conversation = new Conversation();
        conversation.setParticipants(participants);
        conversation.setListing(listing);
        return conversationRepository.save(conversation);
    }


    public List<Conversation> getUserConversations(User user) {
        List<Conversation> conversations = conversationRepository.findAllByParticipant(user);
        // Добавить логирование для проверки
        logger.info("Conversations found: " + conversations.size());
        return conversations;
    }

    public List<ConversationDTO> getConversationsDTOForUser(User user, Locale locale) {
        List<Conversation> conversations = conversationRepository.findAllByParticipant(user);
        logger.info("Conversations for DTO found: " + conversations.size());
        return conversations.parallelStream()
                .sorted((c1, c2) -> {
                    LocalDateTime date1 = c1.getLastMessage() != null ? c1.getLastMessage().getSentAt() : c1.getCreatedAt();
                    LocalDateTime date2 = c2.getLastMessage() != null ? c2.getLastMessage().getSentAt() : c2.getCreatedAt();
                    return date2.compareTo(date1);
                })
                .map(conv -> convertToDTO(conv, user, locale))
                .collect(Collectors.toList());
    }

    public void notifyConversationUpdate(Long conversationId, User user, Locale locale) {
        Conversation conversation = getConversationById(conversationId);
        ConversationDTO conversationDto = convertToDTO(conversation, user, locale);

        // Определяем, есть ли новые сообщения
        boolean hasNewMessage = conversation.getMessages().stream()
                .anyMatch(msg -> !msg.isRead() && msg.getReceiver().equals(user));

        // Устанавливаем флаг нового сообщения
        conversationDto.setHasNewMessage(hasNewMessage);

        // Отправляем обновление конкретному пользователю
        messagingTemplate.convertAndSendToUser(
                user.getSub(),
                "/queue/conversations.updates",
                conversationDto
        );
    }

    public boolean conversationExists(User user1, User user2) {
        return conversationRepository.existsBetweenUsers(user1, user2);
    }

    public long getUnreadMessageCount(Conversation conversation, User user) {
        // Получаем все непрочитанные сообщения для конкретного разговора и пользователя
        return messageRepository.findByConversationAndReceiverAndReadFalse(conversation, user).size();
    }

    public List<Message> getMessages(Conversation conversation) {
        return messageRepository.findByConversationOrderBySentAtAsc(conversation);
    }

    @Transactional
    public Message sendMessage(Conversation conversation, User sender, String text) {
        // Проверка и инициализация списка сообщений, если он пуст или еще не загружен
        if (conversation.getMessages() == null) {
            conversation.setMessages(new ArrayList<>());  // Инициализация списка сообщений, если он пуст
        } else {
            // Явная инициализация коллекции сообщений, если она использует ленивую загрузку
            conversation.getMessages().size();
        }

        // Создание нового сообщения
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReceiver(conversation.getInterlocutor(sender));
        message.setText(text);
        message.setSentAt(LocalDateTime.now());

        // Добавление сообщения в список сообщений беседы
        conversation.getMessages().add(message);

        // Сохраняем сообщение
        messageRepository.save(message);

        // Сохраняем изменения в беседе (если коллекция сообщений была обновлена)
        conversationRepository.save(conversation);

        return message;
    }

    public Conversation getConversationById(Long conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }
    @Transactional
    public void markMessagesAsRead(Long conversationId, User reader) {
        messageRepository.markMessagesAsRead(conversationId, reader.getId());
    }

    public void updateDialogs() {

    }

    public ConversationDTO convertToDTO(Conversation conversation, User currentUser, Locale locale) {
        logger.info("Конвертация в дто начата разговора: " + conversation.getId());
        User interlocutor = conversation.getInterlocutor(currentUser);
        
        logger.info("Юзер найден, конвертируем");

        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setInterlocutorName(interlocutor.getName());
        dto.setInterlocutorAvatar(interlocutor.getAvatarUrl());
        dto.setUnreadCount(getUnreadMessageCount(conversation, currentUser));
        dto.setListing(listingService.convertToDTO(conversation.getListing(), locale));

        logger.info("Обработка последнего сообщения");
        // Обработка последнего сообщения
        Message lastMessage = conversation.getLastMessage();
        if (lastMessage != null) {
            dto.setLastMessagePreview(lastMessage.getText());
            dto.setLastMessageTime(lastMessage.getSentAt());
            dto.setFormattedLastMessageTime(
                    lastMessage.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm"))
            );
        } else {
            dto.setLastMessageTime(conversation.getCreatedAt());
        }

        logger.info("Определяем, есть ли новые сообщения");

        // Определяем, есть ли новые сообщения
        boolean hasNewMessage = conversation.getMessages().stream()
                .filter(msg -> msg.getReceiver().equals(currentUser))
                .anyMatch(msg -> !msg.isRead());
        dto.setHasNewMessage(hasNewMessage);

        logger.info("Конвертация закончена");

        return dto;
    }
}
