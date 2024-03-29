package ru.timeslot.telegram.bot.service.state;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.exception.CommandNotFoundException;
import ru.timeslot.telegram.bot.service.SessionStateService;

import java.util.List;

import static ru.timeslot.telegram.bot.dto.ResponseMessageType.SEND_MESSAGE;
import static ru.timeslot.telegram.bot.dto.TextConstant.TOGGLE_COMMAND;

public abstract class AbstractMenuStateService implements SessionStateService {
    protected MessageResponse getSimpleMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        return MessageResponse.builder()
                .messageType(SEND_MESSAGE)
                .dto(List.of(sendMessage))
                .build();
    }

    protected String extractCommand(String message) {
        if (message.contains(TOGGLE_COMMAND)) {
            return TOGGLE_COMMAND;
        } else if (message.startsWith("/")) {
            return message;
        }
        throw new CommandNotFoundException(message);
    }
}
