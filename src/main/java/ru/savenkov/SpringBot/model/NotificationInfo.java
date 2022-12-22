package ru.savenkov.SpringBot.model;


import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.repository.Query;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity(name = "notifications_table")
public class NotificationInfo{
    //@ToString, @EqualsAndHashCode, @Getter on all fields, @Setter on all non-final fields, and @RequiredArgsConstructor

    //оставить ли оповещение на будущее (опционально, надо будет добавить опцию, при которой пользователь может удалять уведомления)
    @Id

    private UUID id;
    @Column(name = "day_of_week")
    @Nullable                                 //Monday      Tuesday Wednesday Thursday Friday  Saturday Sunday
                                              //Понедельник Вторник Среда     Четвег   Пятница Суббота  Воскресенье
    private DayOfWeek dayOfWeek;
    @Column(name = "notification_text")
    @Nullable
    private String notificationText;
    @Column(name = "notification_time")
    @Nullable
    private LocalTime notificationTime;
    @Column(name = "chat_id")
    @Nullable
    private Long chatId;
    @Column(name = "user_name")
    @Nullable
    private String userName;

    public NotificationInfo() {
    }

    public NotificationInfo(UUID id, @Nullable DayOfWeek dayOfWeek, @Nullable String notificationText, @Nullable LocalTime notificationTime, @Nullable Long chatId, @Nullable String userName) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.notificationText = notificationText;
        this.notificationTime = notificationTime;
        this.chatId = chatId;
        this.userName = userName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getNotificationText() {
        return notificationText;
    }

    public void setNotificationText(String notificationText) {
        this.notificationText = notificationText;
    }

    public LocalTime getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(LocalTime notificationTime) {
        this.notificationTime = notificationTime;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NotificationInfo{");
        sb.append("id=").append(id);
        sb.append(", dayOfWeek=").append(dayOfWeek);
        sb.append(", notificationText='").append(notificationText).append('\'');
        sb.append(", notificationTime=").append(notificationTime);
        sb.append(", chatId=").append(chatId);
        sb.append(", userName='").append(userName).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
