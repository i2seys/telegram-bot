package ru.savenkov.SpringBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.savenkov.SpringBot.config.BotConfig;
import ru.savenkov.SpringBot.model.Condition;
import ru.savenkov.SpringBot.model.NotificationInfo;
import ru.savenkov.SpringBot.model.UserCondition;
import ru.savenkov.SpringBot.repository.NotificationInfoRepository;
import ru.savenkov.SpringBot.repository.UserConditionRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserConditionRepository userConditionRepository;
    @Autowired
    private NotificationInfoRepository notificationInfoRepository;
    final BotConfig config;
    public TelegramBot(BotConfig config){
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "получить приветственное сообщение"));
        listOfCommands.add(new BotCommand("/createnotification", "создать новое уведомление"));
        listOfCommands.add(new BotCommand("/deletenotification", "удалить уведомление"));
        listOfCommands.add(new BotCommand("/shownotification", "показать уведомления"));
        listOfCommands.add(new BotCommand("/help", "информация как использовать этого бота"));
        try {
            execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            //ЕСЛИ ПОЛЬЗОВАТЕЛЬ ХОЧЕТ ОСТАНОВИТЬ СВОЮ ЦЕПОЧКУ ОПЕРАЦИЙ ПО СОЗДАНИЮ / УДАЛЕНИЮ ОПОВЕЩЕНИЯ
            if(messageText.equals("/stop") && userConditionRepository.existsById(chatId)){
                UserCondition userCondition = userConditionRepository.findById(chatId).get();
                switch (userCondition.getCondition()){
                    case ASK_DAY_WEEK:
                    case ASK_NOTIFICATION_TEXT:
                    case ASK_NOTIFICATION_TIME:
                        //удалить оповещение по uuid и удалить состояние
                        userConditionRepository.deleteById(chatId);
                        notificationInfoRepository.deleteById(userCondition.getNotificationId());
                        sendMessage(chatId, "Выход прошёл успешно.");
                        break;
                    case SELECT_NOTIFICATION_TO_DELETE:
                        //просто удалить состояние
                        userConditionRepository.deleteById(chatId);
                        sendMessage(chatId, "Выход прошёл успешно.");
                        break;
                    default:
                        sendMessage(chatId, "Ошибка");
                        break;
                }
            }
            //ЕСЛИ ДАННЫЙ CHAT_ID ЕСТЬ В БАЗЕ ДАННЫХ С СОСТОЯНИЯМИ, ТО
            else if(userConditionRepository.existsById(chatId)){
                UserCondition userCondition = userConditionRepository.findById(chatId).get();
                switch (userCondition.getCondition()){
                    case ASK_DAY_WEEK:
                    case ASK_NOTIFICATION_TEXT:
                    case ASK_NOTIFICATION_TIME:
                        createNotificationProcess(update, userCondition);
                        break;
                    case SELECT_NOTIFICATION_TO_DELETE:
                        deleteNotificationProcess(update);
                        break;
                    default:
                        sendMessage(chatId, "Ошибка");
                        break;
                }
            }
            else{//если же этого chat_id нет, то для него определяются вот такие стартовые команды
                beginCommandsProcess(update);
            }
        }
    }

    private void sendMessage(long chatId, String message){
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(message);
            sendMessage.setChatId(chatId);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void beginCommandsProcess(Update update){
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        switch (messageText){
            case "/start":
                sendMessage(chatId, "Привет!");
                break;
            case "/createnotification":
                UUID notificationId = UUID.randomUUID();
                //задаём состояние для пользователя, что он начал создавать событие
                userConditionRepository.save(new UserCondition(chatId, Condition.ASK_DAY_WEEK, notificationId));

                //создаём событие, которое надо до конца инициализировать
                notificationInfoRepository.save(new NotificationInfo(notificationId, null, null, null, chatId, update.getMessage().getChat().getUserName()));
                sendMessage(chatId, """
                        Если вы хотите выйти из режима создания нового оповещения, введите команду /stop.
                        Введите день недели (Понедельник, Вторник, Среда, Четверг, Пятница, Суббота, Воскресенье)
                        """);
                break;
            case "/deletenotification":
                if(!notificationInfoRepository.existsByChatId(chatId)){
                    sendMessage(chatId, "У вас нет уведомлений.");
                }
                else {
                    userConditionRepository.save(new UserCondition(chatId, Condition.SELECT_NOTIFICATION_TO_DELETE, null));
                    sendMessage(chatId, """
                            Если вы хотите выйти из режима удаления оповещения, введите команду /stop.
                            Выберете уведомление для удаления (введите его номер).
                            """);
                    showNotification(chatId);
                }
                break;
            case "/shownotification":
                showNotification(chatId);
                break;
            default:
                sendMessage(chatId, "Команда не поддерживается.");
                break;
        }
    }
    private void createNotificationProcess(Update update, UserCondition userCondition){
        long chatId = update.getMessage().getChatId();
        switch (userCondition.getCondition()){
            case ASK_DAY_WEEK:
                //получаем данные пользователя
                DayOfWeek dayOfWeek = DayOfWeek.valueOf(update.getMessage().getText().toUpperCase());
                UUID currentUuid = userCondition.getNotificationId();

                //сохраняем в базу данных с оповещениями выбор дня недели
                NotificationInfo previousNotification = notificationInfoRepository.findById(currentUuid).get();
                previousNotification.setDayOfWeek(dayOfWeek);
                notificationInfoRepository.save(previousNotification);

                //изменяем состояние
                userCondition.setCondition(Condition.ASK_NOTIFICATION_TIME);
                userConditionRepository.save(userCondition);

                //пустые поля на данный момент - notification time, notification text
                sendMessage(chatId, "Введите время уведомления (чч:мм).");
                break;
            case ASK_NOTIFICATION_TIME:
                //получаем данные пользователя
                LocalTime notificationTime = LocalTime.parse(update.getMessage().getText(), DateTimeFormatter.ISO_LOCAL_TIME);
                currentUuid = userCondition.getNotificationId();

                //сохраняем в базу данных с оповещениями данные о времени
                previousNotification = notificationInfoRepository.findById(currentUuid).get();
                previousNotification.setNotificationTime(notificationTime);
                notificationInfoRepository.save(previousNotification);

                //изменяем состояние
                userCondition.setCondition(Condition.ASK_NOTIFICATION_TEXT);
                userConditionRepository.save(userCondition);

                //пустые поля на данный момент - notification text
                sendMessage(chatId, "Введите текст вашего уведомления.");
                break;
            case ASK_NOTIFICATION_TEXT:
                //получаем данные от пользователя
                String notificationText = update.getMessage().getText();
                currentUuid = userCondition.getNotificationId();

                //сохраняем в базу данных с оповещениями данные о событии
                previousNotification = notificationInfoRepository.findById(currentUuid).get();
                previousNotification.setNotificationText(notificationText);
                notificationInfoRepository.save(previousNotification);

                //изменяем состояние
                userCondition.setCondition(Condition.ASK_NOTIFICATION_TEXT);
                userConditionRepository.deleteById(chatId);

                sendMessage(chatId, "Готово. Ваше уведомление созданно" + previousNotification);
                break;
        }
    }
    private void deleteNotificationProcess(Update update){
        long chatId = update.getMessage().getChatId();
        int notificationToDeleteIndex = Integer.parseInt(update.getMessage().getText());
        List<NotificationInfo> notificationInfos = notificationInfoRepository.findAllByChatId(chatId);
        UUID uuidOfNotificationToDelete = notificationInfos.get(notificationToDeleteIndex - 1).getId();

        notificationInfoRepository.deleteById(uuidOfNotificationToDelete);
        //закончили с удалением - удаляем состояние бота для текущего пользователя
        userConditionRepository.deleteById(chatId);
        sendMessage(chatId, "Уведомление удалено");
    }
    private void showNotification(long chatId){
        if(notificationInfoRepository.existsByChatId(chatId)){
            List<NotificationInfo> userNotifications = notificationInfoRepository.findAllByChatId(chatId);
            StringBuilder userNotificationsStringBuilder = new StringBuilder("Ваши оповещения: \n");
            for(int i = 0; i < userNotifications.size(); i++){
                String notification = String.format("Оповещение %d: %s %s - %s%n" ,
                        i+1,
                        userNotifications.get(i).getDayOfWeek().toString(),
                        userNotifications.get(i).getNotificationTime().toString(),
                        userNotifications.get(i).getNotificationText());
                userNotificationsStringBuilder.append(notification);
            }
            sendMessage(chatId, userNotificationsStringBuilder.toString());
        }
        else {
            sendMessage(chatId, "У вас нет уведомлений.");
        }
    }
}
