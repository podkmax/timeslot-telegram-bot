package ru.timeslot.telegram.bot.service.state;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.timeslot.parser.dto.TimeslotDto;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.exception.CommandNotFoundException;
import ru.timeslot.telegram.bot.exception.StevedoreUpdateException;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;
import ru.timeslot.telegram.bot.service.TimeslotService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static ru.timeslot.telegram.bot.dto.ResponseMessageType.SEND_MESSAGE;
import static ru.timeslot.telegram.bot.dto.TextConstant.BACK_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.GET_TIMESLOTS_COMMAND;

@Service
@RequiredArgsConstructor
public class TimeSlotMenuStateService extends AbstractMenuStateService {

    private final TimeslotService timeslotService;
    private final UserSessionRepository userSessionRepository;

    private final Map<String, Function<UserSession, MessageResponse>> commands = Map.of(
            GET_TIMESLOTS_COMMAND, getTimeSlotButtons(),
//            SUBSCRIBE_COMMAND, subscribe(),
            BACK_COMMAND, goBack()
    );

    @Override
    public MessageResponse getDefaultResponse(UserSession userSession) {
        return getSimpleMessage(
                userSession.getChatId(),
                String.format("Команда «%s» не распознана. Нажмите одну из представленных кнопок", userSession.getLastMessageText())
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
                    userSession.getChatId(), "Ошибка получения списка доступных Культур. Сервер недоступен."
            );
        } catch (CommandNotFoundException e) {
            return getDefaultResponse(userSession);
        }
        return null;
    }

    private Function<UserSession, MessageResponse> getTimeSlotButtons() {
        return session -> {
            InlineKeyboardMarkup stevedoreList = getTimeslotKeyboard();
            List<Object> messages = getTimeslotMessages(session);
            if (messages.isEmpty()) {
                MessageResponse timeslotEmptyMessage = getSimpleMessage(session.getChatId(), "Нет доступных тайм-слотов на ближайшие 7 дней.");
                ((SendMessage) timeslotEmptyMessage.getDto().getLast()).setReplyMarkup(stevedoreList);
                return timeslotEmptyMessage;
            } else {
                ((SendMessage) messages.getLast()).setReplyMarkup(stevedoreList);
                return MessageResponse.builder()
                        .messageType(SEND_MESSAGE)
                        .dto(messages)
                        .build();
            }
        };
    }

    private List<Object> getTimeslotMessages(UserSession session) {
        List<TimeslotDto> allTimeslots = timeslotService.getAllTimeslots();
        List<Object> messages = new ArrayList<>();
        for (TimeslotDto timeslotDto : allTimeslots) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(session.getChatId()));
            message.setText(
                    String.format(
                            "Стивидор: %s, Экспортер: %s, Культура: %s, Дата: %s",
                            timeslotDto.getStevedore().getName(),
                            timeslotDto.getExporter().getName(),
                            timeslotDto.getCrop().getName(),
                            timeslotDto.getTimeslotDate()
                    )
            );
            messages.add(message);
        }
        return messages;
    }

    private Function<UserSession, MessageResponse> goBack() {
        return session -> {
            session.setSessionState(SessionState.MAIN_MENU);
            userSessionRepository.save(session);
            return null;
        };
    }

    private InlineKeyboardMarkup getTimeslotKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> backLine = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData("/back");
        backLine.add(backButton);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(backLine);
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }
}
