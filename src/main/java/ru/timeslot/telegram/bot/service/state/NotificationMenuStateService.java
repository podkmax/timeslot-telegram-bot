package ru.timeslot.telegram.bot.service.state;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.User;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.exception.CommandNotFoundException;
import ru.timeslot.telegram.bot.exception.StevedoreUpdateException;
import ru.timeslot.telegram.bot.repository.UserRepository;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static ru.timeslot.telegram.bot.dto.ResponseMessageType.SEND_MESSAGE;
import static ru.timeslot.telegram.bot.dto.TextConstant.BACK_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.GET_NOTIFICATION_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.SUBSCRIBE_COMMAND;

@Service
@RequiredArgsConstructor
public class NotificationMenuStateService extends AbstractMenuStateService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final Map<String, Function<UserSession, MessageResponse>> commands = Map.of(
            GET_NOTIFICATION_COMMAND, getNotificationKeyBoard(),
            SUBSCRIBE_COMMAND, subscribe(),
            BACK_COMMAND, goBack()
    );

    @Override
    public MessageResponse getDefaultResponse(UserSession userSession) {
        return getSimpleMessage(
                userSession.getChatId(),
                String.format("Команда «%s» не распознана. Нажмите одну из представленных кнопок",  userSession.getLastMessageText())
        );
    }

    @Override
    public MessageResponse processCommand(UserSession userSession, String message) {
        try {
            String command = extractCommand(message);
            if (commands.containsKey(command)) {
                return commands.get(command).apply(userSession);
            }
        } catch (StevedoreUpdateException e) {
            return getSimpleMessage(
                    userSession.getChatId(),"Ошибка получения списка доступных Культур. Сервер недоступен."
            );
        } catch (CommandNotFoundException e) {
            return getDefaultResponse(userSession);
        }
        return null;
    }

    private Function<UserSession, MessageResponse> subscribe() {
        return session -> {
            User user = userRepository.findByUserId(session.getUserId()).orElseThrow(
                    () -> new EntityNotFoundException(
                            String.format("Пользователь с ID: %s не найден", session.getUserId())
                    )
            );
            user.setSubscribe(!user.getSubscribe());
            userRepository.save(user);
            InlineKeyboardMarkup subscribeKeyboard = getSubscribeKeyboard(user);
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(session.getChatId()));
            message.setText("По кнопке вы можете включить или отключить уведомления о новых тайм-слотах");
            message.setReplyMarkup(subscribeKeyboard);
            return MessageResponse.builder()
                    .messageType(SEND_MESSAGE)
                    .dto(List.of(message))
                    .build();
        };
    }

    private Function<UserSession, MessageResponse> getNotificationKeyBoard() {
        return session -> {
            User user = userRepository.findByUserId(session.getUserId()).orElseThrow(
                    () -> new EntityNotFoundException(
                            String.format("Пользователь с ID: %s не найден", session.getUserId())
                    )
            );
            InlineKeyboardMarkup subscribeKeyboard = getSubscribeKeyboard(user);
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(session.getChatId()));
            message.setText("По кнопке вы можете включить или отключить уведомления о новых тайм-слотах");
            message.setReplyMarkup(subscribeKeyboard);
            return MessageResponse.builder()
                    .messageType(SEND_MESSAGE)
                    .dto(List.of(message))
                    .build();
        };
    }

    private Function<UserSession, MessageResponse> goBack() {
        return session -> {
            session.setSessionState(SessionState.MAIN_MENU);
            userSessionRepository.save(session);
            return null;
        };
    }

    private InlineKeyboardMarkup getSubscribeKeyboard(User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton subscribeButton = new InlineKeyboardButton();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        subscribeButton.setText(user.getSubscribe() ? "Отписаться" : "Подписаться");
        subscribeButton.setCallbackData("/subscribe");
        backButton.setText("Назад");
        backButton.setCallbackData("/back");
        List<InlineKeyboardButton> subscribeLine = new ArrayList<>();
        List<InlineKeyboardButton> backLine = new ArrayList<>();
        subscribeLine.add(subscribeButton);
        subscribeLine.add(backButton);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(subscribeLine);
        rowList.add(backLine);
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }
}
