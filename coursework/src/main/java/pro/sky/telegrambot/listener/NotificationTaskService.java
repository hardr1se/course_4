package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.enities.NotificationTask;
import pro.sky.telegrambot.mapper.NotificationTaskMapper;
import pro.sky.telegrambot.repositories.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class NotificationTaskService {
    private final Logger logger = LoggerFactory.getLogger(NotificationTaskService.class);
    private final NotificationTaskRepository taskRepository;
    private final TelegramBot bot;
    private final NotificationTaskMapper taskMapper;

    public NotificationTaskService(NotificationTaskRepository taskRepository, TelegramBot bot, NotificationTaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.bot = bot;
        this.taskMapper = taskMapper;
    }

    @Scheduled(fixedDelay = 10_000)
    private synchronized void reminder() {
        NotificationTask notificationTask = taskRepository.getByDateOfTask(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        if (notificationTask != null) {
            bot.execute(new SendMessage(notificationTask.getId(), taskMapper.toDto(notificationTask).toString()));
            taskRepository.delete(notificationTask);
            logger.info("Task '" + notificationTask + "' is pushed and deleted from DB");
        }
    }
}
