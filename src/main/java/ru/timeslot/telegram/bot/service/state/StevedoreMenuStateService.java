package ru.timeslot.telegram.bot.service.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.timeslot.parser.dto.StevedoreDto;
import ru.timeslot.telegram.bot.client.TimeslotParserClient;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.exception.CommandNotFoundException;
import ru.timeslot.telegram.bot.exception.StevedoreUpdateException;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;
import ru.timeslot.telegram.bot.service.StevedoreService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.toIntExact;
import static ru.timeslot.telegram.bot.dto.ResponseMessageType.EDIT_MESSAGE;
import static ru.timeslot.telegram.bot.dto.ResponseMessageType.SEND_MESSAGE;
import static ru.timeslot.telegram.bot.dto.TextConstant.BACK_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.GET_STEVEDORES_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.GREEN;
import static ru.timeslot.telegram.bot.dto.TextConstant.RED;
import static ru.timeslot.telegram.bot.dto.TextConstant.TOGGLE_COMMAND;

@Slf4j
@Service
@RequiredArgsConstructor
public class StevedoreMenuStateService extends AbstractMenuStateService {

    private final TimeslotParserClient timeslotParserClient;
    private final UserSessionRepository userSessionRepository;
    private final StevedoreService stevedoreService;

    private static final String BUTTON_MESSAGE = "Список доступных Стивидоров. Зелёным обозначены активные Стивидоры. Чтобы выключить запрос тайм-слотов по стивидору, нажмите на кнопку с названием Стивидора.";

    private final Map<String, Function<UserSession, MessageResponse>> commands = Map.of(
            GET_STEVEDORES_COMMAND, getStevedoresButtons(),
            TOGGLE_COMMAND, toggleStevedore(),
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
            return null;
        } catch (StevedoreUpdateException e) {
            return getSimpleMessage(
                    userSession.getChatId(),"Ошибка получения списка доступных Стивидоров. Сервер недоступен."
            );
        } catch (CommandNotFoundException e) {
            return getDefaultResponse(userSession);
        }
    }

    private Function<UserSession, MessageResponse> getStevedoresButtons() {
        return userSession -> {
            InlineKeyboardMarkup stevedoreList = getStevedoreKeyboard();

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(userSession.getChatId()));
            message.setText(BUTTON_MESSAGE);
            message.setReplyMarkup(stevedoreList);
            return MessageResponse.builder()
                    .messageType(SEND_MESSAGE)
                    .dto(List.of(message))
                    .build();
        };
    }

    private Function<UserSession, MessageResponse> toggleStevedore() {
        return userSession -> {
            InlineKeyboardMarkup stevedoreList = getStevedoreKeyboard();
            stevedoreList.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .filter(b -> b.getCallbackData().equals(userSession.getLastMessageText()))
                    .forEach(b -> {
                        StevedoreDto stevedoreDto = stevedoreService.getStevedores()
                                .get(Integer.parseInt(b.getCallbackData().split("/")[3]));
                        b.setText(
                                Boolean.TRUE.equals(stevedoreDto.getActive())
                                ? RED + stevedoreDto.getName()
                                : GREEN + stevedoreDto.getName()
                        );
                        timeslotParserClient.toggleStevedore(stevedoreDto.getId());
                        stevedoreDto.setActive(!stevedoreDto.getActive());
                    });
            EditMessageText editedMessage = new EditMessageText();
            editedMessage.setChatId(userSession.getChatId());
            editedMessage.setMessageId(toIntExact(userSession.getLastMessageId()));
            editedMessage.setReplyMarkup(stevedoreList);
            editedMessage.setText(BUTTON_MESSAGE);
            return MessageResponse.builder()
                    .messageType(EDIT_MESSAGE)
                    .dto(List.of(editedMessage))
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

    private InlineKeyboardMarkup getStevedoreKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> stevedoreLine = new ArrayList<>();
        List<InlineKeyboardButton> backLine = new ArrayList<>();
        InlineKeyboardButton stevedoreButton;
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData("/back");
        backLine.add(backButton);
        Map<Integer, StevedoreDto> stevedores = stevedoreService.getStevedores();
        for (StevedoreDto stevedoreDto : stevedores.values()) {
            boolean color = stevedoreDto.getActive();
            stevedoreButton = new InlineKeyboardButton();
            stevedoreButton.setText(color ? GREEN + stevedoreDto.getName() : RED + stevedoreDto.getName());
            stevedoreButton.setCallbackData("/toggle/activate/" + stevedoreDto.getId());
            stevedoreLine.add(stevedoreButton);
        }

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(stevedoreLine);
        rowList.add(backLine);
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }
}
