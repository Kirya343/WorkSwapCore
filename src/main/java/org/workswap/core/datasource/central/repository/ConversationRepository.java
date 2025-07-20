package org.workswap.core.datasource.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.workswap.core.datasource.central.model.Listing;
import org.workswap.core.datasource.central.model.User;
import org.workswap.core.datasource.central.model.chat.Conversation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Найти разговор между двумя пользователями
    @Query("SELECT c FROM Conversation c WHERE :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants AND SIZE(c.participants) = 2")
    Optional<Conversation> findBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    // Найти все разговоры пользователя
    @Query("SELECT c FROM Conversation c WHERE :user MEMBER OF c.participants")
    List<Conversation> findAllByParticipant(@Param("user") User user);

    // Найти разговор между двумя пользователями и объявлением
    @Query("SELECT c FROM Conversation c WHERE :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants AND c.listing = :listing AND SIZE(c.participants) = 2")
    Optional<Conversation> findBetweenUsersAndListing(@Param("user1") User user1, @Param("user2") User user2, @Param("listing") Listing listing);

    // Проверка существования разговора между двумя пользователями
    @Query("SELECT COUNT(c) > 0 FROM Conversation c WHERE :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants AND SIZE(c.participants) = 2")
    boolean existsBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    List<Conversation> findAllByListing(Listing listing);
}
