package ru.timeslot.telegram.bot.service.state;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.timeslot.telegram.bot.domain.AccessRequest;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.dto.ResponseMessageType;
import ru.timeslot.telegram.bot.repository.AccessRequestRepository;
import ru.timeslot.telegram.bot.service.SessionStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AccessDeniedStateService implements SessionStateService {

    private final AccessRequestRepository accessRequestRepository;
    private final Map<String, Function<UserSession, MessageResponse>> commands = Map.of(
            "/get_access", getAccessCommand()
    );

    @Override
    public MessageResponse getDefaultResponse(UserSession userSession) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton accessRequestButton = new InlineKeyboardButton();

        accessRequestButton.setText("Запросить доступ");
        accessRequestButton.setCallbackData("/get_access");

        List<InlineKeyboardButton> accessRequest = new ArrayList<>();
        accessRequest.add(accessRequestButton);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(accessRequest);
        inlineKeyboardMarkup.setKeyboard(rowList);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(userSession.getChatId()));
        message.setText("Доступ запрещён, обратитесь к администратору");
        message.setReplyMarkup(inlineKeyboardMarkup);
        return MessageResponse.builder()
                .messageType(ResponseMessageType.SEND_MESSAGE)
                .dto(List.of(message))
                .build();
    }

    @Override
    public MessageResponse processCommand(UserSession userSession, String message) {
        Optional<AccessRequest> optionalAccessRequest = accessRequestRepository.findByUserId(userSession.getUserId());
        if (optionalAccessRequest.isPresent()) {
            return getSimpleResponse(
                    userSession.getChatId(),
                    "Запрос на получение доступа ожидает подтверждения администратором");
        }
        if (commands.containsKey(message)) {
            return commands.get(message).apply(userSession);
        } else {
            return getDefaultResponse(userSession);
        }
    }

    private Function<UserSession, MessageResponse> getAccessCommand() {
        return (session) ->  {
            AccessRequest accessRequest = AccessRequest.builder().userId(session.getUserId()).build();
            accessRequestRepository.save(accessRequest);
            return getSimpleResponse(session.getChatId(), "Доступ запрошен. Ожидайте оповещения администратора.");
        };
    }


    private MessageResponse getSimpleResponse(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        return MessageResponse.builder()
                .messageType(ResponseMessageType.SEND_MESSAGE)
                .dto(List.of(message))
                .build();
    }
}
