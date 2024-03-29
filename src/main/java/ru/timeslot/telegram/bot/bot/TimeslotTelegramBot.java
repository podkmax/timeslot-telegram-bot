package ru.timeslot.telegram.bot.bot;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.timeslot.telegram.bot.configuration.BotProperties;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.dto.ResponseMessageType;
import ru.timeslot.telegram.bot.event.TimeslotEvent;
import ru.timeslot.telegram.bot.service.SessionService;
import ru.timeslot.telegram.bot.service.SessionStateService;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component
public class TimeslotTelegramBot extends TelegramLongPollingBot {

    private final BotProperties botProperties;
    private final SessionService sessionService;


    private final Map<SessionState, SessionStateService> stateHandlers;

    public TimeslotTelegramBot(
            BotProperties botProperties,
            SessionService sessionService,
            Map<SessionState, SessionStateService> stateHandlers
    ) {
        super(botProperties.botToken());
        this.botProperties = botProperties;
        this.sessionService = sessionService;
        this.stateHandlers = stateHandlers;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage() != null) {
            Long userId = update.getMessage().getFrom().getId();
            Long chatId = update.getMessage().getChatId();
            Integer messageId = update.getMessage().getMessageId();
            String message = update.getMessage().getText();
            handleMessage(userId, chatId, message, messageId);
        } else if (update.getCallbackQuery() != null) {
            Long userId = update.getCallbackQuery().getFrom().getId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String message = update.getCallbackQuery().getData();
            handleMessage(userId, chatId, message, messageId);
        }
    }

    private void handleMessage(Long userId, Long chatId, String message, Integer messageId) {
        UserSession userSession = sessionService.getOrCreateUserSession(userId, chatId, messageId, message);
        SessionStateService sessionStateService = stateHandlers.get(userSession.getSessionState());
        MessageResponse messageResponse = sessionStateService.processCommand(userSession, message);
        if (isNull(messageResponse)) {
            messageResponse = stateHandlers.get(userSession.getSessionState()).processCommand(userSession, message);
        }
        sendMessage(messageResponse);
    }

    private void sendMessage(@Nullable MessageResponse messageResponse) {
        try {
            if (nonNull(messageResponse)) {
                if (messageResponse.getMessageType() == ResponseMessageType.SEND_MESSAGE) {
                    for (Object message : messageResponse.getDto()) {
                        execute( (SendMessage) message);
                    }
                } else if (messageResponse.getMessageType() == ResponseMessageType.EDIT_MESSAGE) {
                    for (Object message : messageResponse.getDto()) {
                        execute( (EditMessageText) message);
                    }
                }
            }
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения", e);
        }
    }

    @EventListener
    public void sendTimeslotNotification(TimeslotEvent event) {
        event.getUserWithChatId().forEach((key, value) -> {
            if (nonNull(value)) {
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(value));
                message.setText(String.format("Новый тайм-слот! %s", event.getMessage()));
                MessageResponse messageResponse = MessageResponse.builder()
                        .messageType(ResponseMessageType.SEND_MESSAGE)
                        .dto(List.of(message))
                        .build();
                sendMessage(messageResponse);
            }
        });
    }

    @Override
    public String getBotUsername() {
        return botProperties.botName();
    }
}
