package org.workswap.core.services.impl;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.workswap.core.services.ChatService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.common.dto.chat.ChatDTO;
import org.workswap.datasource.central.model.chat.Chat;
import org.workswap.datasource.central.model.chat.Message;
import org.workswap.datasource.central.repository.chat.ChatParticipantRepository;
import org.workswap.datasource.central.repository.chat.ChatRepository;
import org.workswap.datasource.central.repository.chat.MessageRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Profile("production")
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Chat getOrCreateChat(Set<User> participants, Listing listing) {
        if (participants.size() != 2) {
            throw new IllegalArgumentException("Chat must have exactly 2 participants.");
        }

        Iterator<User> iterator = participants.iterator();
        User user1 = iterator.next();
        User user2 = iterator.next();

        // Сначала ищем чат с привязкой к объявлению
        if (listing != null) {
            logger.debug("Объявление есть");
            Optional<Chat> existing = chatParticipantRepository.findChatBetweenUsers(user1, user2, listing);
            if (existing.isPresent()) {
                logger.debug("Объявление: {}", listing.getId());
                logger.debug("Нашли чат с объявлением");
                return existing.get();
            }
        } else {
            // Ищем общий чат без привязки к объявлению
            logger.debug("Объявления нет");
            Optional<Chat> existing = chatParticipantRepository.findChatBetweenUsers(user1, user2, null);
            if (existing.isPresent()) {
                logger.debug("Нашли чат без объявления");
                return existing.get();
            }
        }

        logger.debug("Чатов нет, создём новый");
        // Создаём новый
        Chat chat = new Chat(participants, listing);
        return chatRepository.save(chat);
    }

    @Override
    public List<Chat> getUserChats(User user) {
        List<Chat> chats = chatRepository.findAllByParticipant(user);
        // Добавить логирование для проверки
        logger.debug("Chats found: " + chats.size());
        return chats;
    }

    @Override
    @Transactional
    public List<ChatDTO> getChatsDTOForUser(User user, Locale locale) {
        List<Chat> chats = chatRepository.findAllByParticipant(user);
        logger.debug("Chats for DTO found: " + chats.size());
        return chats.parallelStream()
                .sorted((c1, c2) -> {
                    LocalDateTime date1 = c1.getLastMessage() != null ? c1.getLastMessage().getSentAt() : c1.getCreatedAt();
                    LocalDateTime date2 = c2.getLastMessage() != null ? c2.getLastMessage().getSentAt() : c2.getCreatedAt();
                    return date2.compareTo(date1);
                })
                .map(conv -> convertToDTO(conv, user, locale))
                .toList();
    }

    @Override
    public void notifyChatUpdate(Long chatId, User user, Locale locale) {
        Chat chat = getChatById(chatId);
        ChatDTO chatDto = convertToDTO(chat, user, locale);

        // Определяем, есть ли новые сообщения
        boolean hasNewMessage = chat.getMessages().stream()
                .anyMatch(msg -> !msg.isRead() && msg.getReceiver().equals(user));

        // Устанавливаем флаг нового сообщения
        chatDto.setHasNewMessage(hasNewMessage);

        // Отправляем обновление конкретному пользователю
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "queue/chats.updates",
                chatDto
        );
    }

    @Override
    public boolean chatExists(User user1, User user2) {
        return chatParticipantRepository.existsBetweenUsers(user1, user2);
    }

    @Override
    public long getUnreadMessageCount(Chat chat, User user) {
        // Получаем все непрочитанные сообщения для конкретного разговора и пользователя
        return messageRepository.findByChatAndReceiverAndReadFalse(chat, user).size();
    }

    @Override
    public List<Message> getMessages(Chat chat) {
        return messageRepository.findByChatOrderBySentAtAsc(chat);
    }

    @Override
    @Transactional
    public Message sendMessage(Chat chat, User sender, String text) {

        Message message = new Message(chat, sender, chat.getInterlocutor(sender), text);

        messageRepository.save(message);

        return message;
    }

    @Override
    public Chat getChatById(Long chatId) {
        return chatRepository.findById(chatId).orElse(null);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long chatId, User reader) {
        messageRepository.markMessagesAsRead(chatId, reader.getId());
    }

    @Override
    public ChatDTO convertToDTO(Chat chat, User currentUser, Locale locale) {
        logger.debug("Конвертация в дто начата разговора: " + chat.getId());

        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setUnreadCount(getUnreadMessageCount(chat, currentUser));
        dto.setTemporary(chat.isTemporary());

        logger.debug("Обработка последнего сообщения");
        // Обработка последнего сообщения
        Message lastMessage = chat.getLastMessage();
        if (lastMessage != null) {
            dto.setLastMessagePreview(lastMessage.getText());
            dto.setLastMessageTime(lastMessage.getSentAt());
            dto.setFormattedLastMessageTime(
                    lastMessage.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm"))
            );
        } else {
            dto.setLastMessageTime(chat.getCreatedAt());
        }

        logger.debug("Определяем, есть ли новые сообщения");

        // Определяем, есть ли новые сообщения
        boolean hasNewMessage = chat.getMessages().stream()
                .filter(msg -> msg.getReceiver().equals(currentUser))
                .anyMatch(msg -> !msg.isRead());
        dto.setHasNewMessage(hasNewMessage);

        logger.debug("Конвертация закончена");

        return dto;
    }

    @Override
    public void setPermanentChat(Chat chat) {
        chat.setTemporary(false);
        chatRepository.save(chat);
    }

    @Override
    @Transactional
    public void deleteChat(Chat chat) {
        
        logger.debug("Начинаем удаление чата {}", chat.getId());
        List<Message> messages = chat.getMessages();

        logger.debug("> Чистим сообщения");

        if (!messages.isEmpty()) {
            for(Message msg : messages) {
                logger.debug(">> Удаляем сообщение {}", msg.getId());
                messageRepository.delete(msg);
            }
        } else {
            logger.debug("> В чате не было сообщений");
        }

        logger.debug("Удаляем чат");
        
        chatRepository.delete(chat);
    }
}
