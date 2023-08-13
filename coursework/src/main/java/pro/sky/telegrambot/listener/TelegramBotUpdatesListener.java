package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.dto.NotificationTaskDtoIn;
import pro.sky.telegrambot.mapper.NotificationTaskMapper;
import pro.sky.telegrambot.repositories.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final static Pattern PATTERN = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{4} \\d{1,2}:\\d{2}) ([А-яA-z\\d,\\s]+)");
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository taskRepository;
    private final NotificationTaskMapper taskMapper;
    private final TelegramBot bot;

    public TelegramBotUpdatesListener(TelegramBot telegramBot,
                                      NotificationTaskRepository taskRepository,
                                      NotificationTaskMapper taskMapper,
                                      TelegramBot bot) {
        this.telegramBot = telegramBot;
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.bot = bot;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            var chatId = update.message().chat().id();
            if (update.message() != null) {
                var text = update.message().text();
                var matcher = PATTERN.matcher(text);
                LocalDateTime date;
                if (text.equals("/start")) {
                    bot.execute(new SendMessage(chatId, "Hi there, It's my course work. To get the pattern of correct task formation print command /add."));
                } else if (text.equals("/add")) {
                    bot.execute(new SendMessage(chatId, "Write ur task according to a certain example: 01.01.2022 20:00 Помыть посуду"));
                } else if (matcher.matches() && (date = parseDate(matcher.group(1))) != null) {
                    NotificationTaskDtoIn dtoIn = new NotificationTaskDtoIn(Math.toIntExact(chatId), text, date);
                    bot.execute(new SendMessage(
                            chatId, "Task '" + taskMapper.toDto(taskRepository.save(taskMapper.toEntity(dtoIn))) + "' is successfully added"));

                } else {
                    logger.error("Unsupported format of message");
                    bot.execute(new SendMessage(chatId, "Unsupported type of file"));
                }
            } else {
                logger.error("Unsupported command");
                bot.execute(new SendMessage(chatId, "Unsupported command"));
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private LocalDateTime parseDate(String date) {
        try {
            return LocalDateTime.parse(date, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
