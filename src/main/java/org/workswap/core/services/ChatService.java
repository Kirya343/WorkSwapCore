package org.workswap.core.services;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.common.dto.ChatDTO;
import org.workswap.datasource.central.model.chat.Chat;
import org.workswap.datasource.central.model.chat.Message;

public interface ChatService {

    Chat getOrCreateChat(Set<User> participants, Listing listing);
    Chat getChatById(Long chatId);
    ChatDTO convertToDTO(Chat chat, User currentUser, Locale locale);

    List<Chat> getUserChats(User user);
    List<ChatDTO> getChatsDTOForUser(User user, Locale locale);

    List<Message> getMessages(Chat chat);
    Message sendMessage(Chat chat, User sender, String text);

    void notifyChatUpdate(Long chatId, User user, Locale locale);
    void markMessagesAsRead(Long chatId, User reader);
    void setPermanentChat(Chat chat);
    void deleteChat(Chat chat);
    
    boolean chatExists(User user1, User user2);
    long getUnreadMessageCount(Chat chat, User user);

}
