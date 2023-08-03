package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.enities.NotificationTask;
import pro.sky.telegrambot.mapper.NotificationTaskMapper;
import pro.sky.telegrambot.repositories.NotificationTaskRepository;
import pro.sky.telegrambot.utils.MessageUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private long chatId = 0;

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository taskRepository;
    private final NotificationTaskMapper taskMapper;
    private final TelegramBot bot;
    private final MessageUtils messageUtils;


    public TelegramBotUpdatesListener(TelegramBot telegramBot,
                                      NotificationTaskRepository taskRepository,
                                      NotificationTaskMapper taskMapper,
                                      TelegramBot bot, MessageUtils messageUtils) {
        this.telegramBot = telegramBot;
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.bot = bot;
        this.messageUtils = messageUtils;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        var pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");

        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            chatId = update.message().chat().id();
            var message = update.message().text();
            var matcher = pattern.matcher(message);

            distributiveMessagesByCommands(chatId, matcher, message);
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void distributiveMessagesByCommands(Long chatId, Matcher matcher, String message) {
        if (message.equals("/start")) {
            processStartCommand();
        } else if (message.equals("/add")) {
            processAddCommand();
        } else if (matcher.matches()) {
            processNotificationTask(message);
        } else {
            unsupportedTypeOfText();
        }
    }

    private void processStartCommand() {
        bot.execute(messageUtils.sendMessage(chatId, "Hi there, It's my course work. To get the pattern of correct task formation print command /add."));
    }

    private void processAddCommand() {
        var sendAddMessage = messageUtils.sendMessage(chatId, "Write ur task according to a certain example: 01.01.2022 20:00 Помыть посуду");
        bot.execute(sendAddMessage);
    }

    private void unsupportedTypeOfText() {
        logger.error("Unsupported command");
        bot.execute(messageUtils.sendMessage(chatId, "Unsupported command"));
    }

    private void processNotificationTask(String message) {
        var taskDtoIn = messageUtils.taskProcessing(chatId, message);
        if (taskDtoIn == null) {
            logger.error("Unsupported argument");
            return;
        }
        bot.execute(messageUtils.sendMessage(chatId, String.format(
                "Task: '%s' - is successfully added",
                taskMapper.toDto(taskRepository.save(taskMapper.toEntity(taskDtoIn))))));
    }

    @Scheduled(fixedDelay = 10_000)
    private synchronized void reminder() {
        if (chatId != 0) {
            NotificationTask notificationTask = taskRepository.getByDateOfTask(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
            if (notificationTask != null) {
                taskRepository.delete(notificationTask);
                bot.execute(messageUtils.sendMessage(chatId, taskMapper.toDto(notificationTask).toString()));
                logger.info("Task '" + notificationTask + "' is pushed and deleted from DB");
            }
        }
    }
}
