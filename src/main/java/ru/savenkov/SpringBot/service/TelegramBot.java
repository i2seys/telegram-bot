package ru.savenkov.SpringBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.savenkov.SpringBot.config.BotConfig;
import ru.savenkov.SpringBot.model.Condition;
import ru.savenkov.SpringBot.model.NotificationInfo;
import ru.savenkov.SpringBot.model.UserCondition;
import ru.savenkov.SpringBot.repository.NotificationInfoRepository;
import ru.savenkov.SpringBot.repository.UserConditionRepository;
import ru.savenkov.SpringBot.util.DayOfWeekFromEnglishToRussian;
import ru.savenkov.SpringBot.util.InputInfoParser;
import ru.savenkov.SpringBot.util.InputInfoValidator;
import ru.savenkov.SpringBot.util.NotificationsSortComparator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final String SHOW_NOTIFICATIONS_COMMAND_NAME = "/shownotifications";
    private final String DELETE_NOTIFICATION_COMMAND_NAME = "/deletenotification";
    private final String CREATE_NOTIFICATION_COMMAND_NAME = "/createnotification";
    @Autowired
    private UserConditionRepository userConditionRepository;
    @Autowired
    private NotificationInfoRepository notificationInfoRepository;
    @Autowired
    private InputInfoValidator inputInfoValidator;
    @Autowired
    private InputInfoParser inputInfoParser;
    @Autowired
    private DayOfWeekFromEnglishToRussian dayConverter;
    private final BotConfig config;
    public TelegramBot(BotConfig config){
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "получить приветственное сообщение"));
        listOfCommands.add(new BotCommand(CREATE_NOTIFICATION_COMMAND_NAME, "создать новое уведомление"));
        listOfCommands.add(new BotCommand(DELETE_NOTIFICATION_COMMAND_NAME, "удалить уведомление"));
        listOfCommands.add(new BotCommand(SHOW_NOTIFICATIONS_COMMAND_NAME, "показать уведомления"));
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
            sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void sendMessageWithDaysChoose(long chatId, String message){
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(message);
            sendMessage.setChatId(chatId);

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboardRows = new ArrayList<>();
            KeyboardRow row1 = new KeyboardRow();
            row1.add("Понедельник");
            row1.add("Вторник");
            row1.add("Среда");
            row1.add("Четверг");
            KeyboardRow row2 = new KeyboardRow();
            row2.add("Пятница");
            row2.add("Суббота");
            row2.add("Воскресенье");

            keyboardRows.add(row1);
            keyboardRows.add(row2);
            keyboardMarkup.setKeyboard(keyboardRows);
            sendMessage.setReplyMarkup(keyboardMarkup);

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
                sendMessage(chatId, String.format("""
                        Для начала взаимодействия с ботом, введите одну из предложенных команд:
                        %s - создать новое уведомление;
                        %s - удалить уведомление;
                        %s - показать уведомления;
                        /help - информация как использовать этого бота.
                        """,CREATE_NOTIFICATION_COMMAND_NAME, DELETE_NOTIFICATION_COMMAND_NAME, SHOW_NOTIFICATIONS_COMMAND_NAME));
                break;
            case CREATE_NOTIFICATION_COMMAND_NAME:
                UUID notificationId = UUID.randomUUID();
                //задаём состояние для пользователя, что он начал создавать событие
                userConditionRepository.save(new UserCondition(chatId, Condition.ASK_DAY_WEEK, notificationId));

                //создаём событие, которое надо до конца инициализировать
                notificationInfoRepository.save(new NotificationInfo(notificationId, null, null, null, chatId, update.getMessage().getChat().getFirstName()));
                sendMessageWithDaysChoose(chatId, """
                        Если вы хотите выйти из режима создания нового оповещения, введите команду /stop.
                        Введите день недели.
                        """);
                break;
            case DELETE_NOTIFICATION_COMMAND_NAME:
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
            case SHOW_NOTIFICATIONS_COMMAND_NAME:
                showNotification(chatId);
                break;
            case "/help":
                sendMessage(chatId, String.format("""
                        Данный бот умеет присылать оповещения о событиях, которые вы ему зададите.
                        За 15 минут до назначенного вами события будет приходить уведомление.
                        После получения вами оповещения, данные о нём будут удалены.
                        Бот умеет:
                        -хранить уведомления;
                        -создавать уведомления;
                        -удалять уведомления;
                        -присылать уведомления.
                        Описание команд:
                        %s - создать новое уведомление;
                        %s - удалить уведомление;
                        %s - показать уведомления.
                        Если вы хотите отменить выполнение какой-то команды (например, создание уведомления), то напишите команду /stop.
                        """,CREATE_NOTIFICATION_COMMAND_NAME, DELETE_NOTIFICATION_COMMAND_NAME, SHOW_NOTIFICATIONS_COMMAND_NAME));
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
                String inputText = update.getMessage().getText();
                if(!inputInfoValidator.dayOfWeekIsValid(inputText)){
                    sendMessageWithDaysChoose(chatId, """
                            Данные о дне недели введены некорректно.
                            Введите одно из предложенного ниже.
                            """);
                    return;
                }
                DayOfWeek dayOfWeek = inputInfoParser.parseDayOfWeek(inputText);
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
                inputText = update.getMessage().getText();
                if(!inputInfoValidator.timeIsValid(inputText)){
                    sendMessage(chatId, "Данные о времени введены некорректно. Пример корректного ввода: 12:50, 9:25, 06:10");
                    return;
                }
                LocalTime notificationTime = inputInfoParser.parseTime(inputText);
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
                if(!inputInfoValidator.notificationTextIsValid(update.getMessage().getText())){
                    sendMessage(chatId, "Данные о событии введены неверно. Строка должна быть не пустой и содержать не более 255 символов");
                    return;
                }
                String notificationText = update.getMessage().getText();
                currentUuid = userCondition.getNotificationId();

                //сохраняем в базу данных с оповещениями данные о событии
                previousNotification = notificationInfoRepository.findById(currentUuid).get();
                previousNotification.setNotificationText(notificationText);
                notificationInfoRepository.save(previousNotification);

                //изменяем состояние
                userCondition.setCondition(Condition.ASK_NOTIFICATION_TEXT);
                userConditionRepository.deleteById(chatId);
                sendMessage(chatId,    String.format("""
                            Готово. Ваше уведомление создано.
                            Для показа уведомлений введите команду %s.
                            """, SHOW_NOTIFICATIONS_COMMAND_NAME));
                break;
        }
    }
    private void deleteNotificationProcess(Update update){
        long chatId = update.getMessage().getChatId();
        List<NotificationInfo> notificationInfos = notificationInfoRepository.findAllByChatId(chatId);
        notificationInfos.sort(new NotificationsSortComparator());
        //проверка на корректность входных данных
        if(!inputInfoValidator.indexOfNotificationToDeleteIsValid(update.getMessage().getText(), notificationInfos.size())){
            sendMessage(chatId, "Номер для удаления введён некорректно. Введите число от 1 до " + notificationInfos.size());
            return;
        }
        int notificationToDeleteIndex = Integer.parseInt(update.getMessage().getText());
        UUID uuidOfNotificationToDelete = notificationInfos.get(notificationToDeleteIndex - 1).getId();

        notificationInfoRepository.deleteById(uuidOfNotificationToDelete);
        //закончили с удалением - удаляем состояние бота для текущего пользователя
        userConditionRepository.deleteById(chatId);
        sendMessage(chatId, "Уведомление удалено.");
    }
    private void showNotification(long chatId){
        if(notificationInfoRepository.existsByChatId(chatId)){
            List<NotificationInfo> userNotifications = notificationInfoRepository.findAllByChatId(chatId);
            userNotifications.sort(new NotificationsSortComparator());
            StringBuilder userNotificationsStringBuilder = new StringBuilder("Ваши оповещения: \n");
            for(int i = 0; i < userNotifications.size(); i++){
                String notification = String.format("Оповещение %d: %s %s - %s%n" ,
                        i+1,
                        dayConverter.parse(userNotifications.get(i).getDayOfWeek()),
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
    @Scheduled(cron = "0 * * * * *")//в 0 секунду каждую минуту каждый день каждый час ... = раз в минуту
    private void sendNotificationByTime(){
        //1) получать список всех уведомлений, у которых notification_time = LocalTime.now().plusMinutes(15);
        List<NotificationInfo> currentNotifications = notificationInfoRepository.findAllByNotificationTime(LocalTime.now().plusMinutes(15));
        DayOfWeek currentDayOfWeek = LocalDate.now().getDayOfWeek();
        currentNotifications = currentNotifications.stream().filter(x -> x.getDayOfWeek() == currentDayOfWeek).toList();

        //2)отправить всем этим chatId сообщения.(если хотя бы одно поле - NULL, то не отправлять)
        for(NotificationInfo notification : currentNotifications){
            if(notification.getChatId() == null ||
                    notification.getId() == null ||
                    notification.getNotificationTime() == null ||
                    notification.getNotificationText() == null ||
                    notification.getUserName() == null ||
                    notification.getDayOfWeek() == null){
                continue;
            }
            sendMessage(notification.getChatId(), String.format("""
                    Приветствуем, %s. Напоминание: через 15 минут у вас будет следующее событие - %s
                    """, notification.getUserName(), notification.getNotificationText()));
        }

        //3)удалить сообщения из базы данных.
        for(NotificationInfo notification : currentNotifications){
            notificationInfoRepository.deleteById(notification.getId());
        }
    }
}
