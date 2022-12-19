package ru.savenkov.SpringBot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity(name = "user_condition")
public class UserCondition {
    @Id
    @Column(name = "chat_id")
    private Long chatId;
    @Column(name = "_condition")
    private Condition condition;
    @Column(name = "notification_id")
    private UUID notificationId;

    public UserCondition() {
    }

    public UserCondition(Long chatId, Condition condition, UUID notificationId) {
        this.chatId = chatId;
        this.condition = condition;
        this.notificationId = notificationId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }
}
