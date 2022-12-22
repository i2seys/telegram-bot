package ru.savenkov.SpringBot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.savenkov.SpringBot.model.NotificationInfo;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationInfoRepository extends CrudRepository<NotificationInfo, UUID> {
    List<NotificationInfo> findAllByNotificationTime(LocalTime notificationTime);
    boolean existsByChatId(Long chatId);
    List<NotificationInfo> findAllByChatId(Long chatId);


    @Query("select n from notifications_table n where n.chatId = ?1")
    NotificationInfo findByChatId(Long chatId);
}
