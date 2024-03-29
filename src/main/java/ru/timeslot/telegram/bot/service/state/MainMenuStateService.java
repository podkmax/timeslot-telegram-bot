package ru.timeslot.telegram.bot.service.state;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;
import ru.timeslot.telegram.bot.service.SessionStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static ru.timeslot.telegram.bot.dto.ResponseMessageType.SEND_MESSAGE;

@Service
@RequiredArgsConstructor
public class MainMenuStateService implements SessionStateService {

    private final UserSessionRepository userSessionRepository;

    private final Map<String, Function<UserSession, MessageResponse>> commands = Map.of(
            "/stevedores", getStevedores(),
            "/crops", getCrops(),
            "/exporters", getExporters(),
            "/timeslots", getTimeslots(),
            "/notification", getNotifications()
    );

    @Override
    public MessageResponse getDefaultResponse(UserSession userSession) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton showStevedores = new InlineKeyboardButton();
        InlineKeyboardButton showCrops = new InlineKeyboardButton();
        InlineKeyboardButton showExporters = new InlineKeyboardButton();
        InlineKeyboardButton showTimeslots = new InlineKeyboardButton();
        InlineKeyboardButton notificationButton = new InlineKeyboardButton();

        showStevedores.setText("Стивидоры");
        showStevedores.setCallbackData("/stevedores");
        showCrops.setText("Культуры");
        showCrops.setCallbackData("/crops");
        showExporters.setText("Экспортеры");
        showExporters.setCallbackData("/exporters");
        showTimeslots.setText("Тайм-слоты");
        showTimeslots.setCallbackData("/timeslots");
        notificationButton.setText("Уведомления о новых тайм-слотах");
        notificationButton.setCallbackData("/notification");

        List<InlineKeyboardButton> showStevedoreButtons = new ArrayList<>();
        List<InlineKeyboardButton> showCropButtons = new ArrayList<>();
        List<InlineKeyboardButton> showExporterButtons = new ArrayList<>();
        List<InlineKeyboardButton> showTimeslotButtons = new ArrayList<>();
        List<InlineKeyboardButton> showNotificationButtons = new ArrayList<>();
        showStevedoreButtons.add(showStevedores);
        showCropButtons.add(showCrops);
        showExporterButtons.add(showExporters);
        showTimeslotButtons.add(showTimeslots);
        showNotificationButtons.add(notificationButton);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(showStevedoreButtons);
        rowList.add(showCropButtons);
        rowList.add(showExporterButtons);
        rowList.add(showTimeslotButtons);
        rowList.add(showNotificationButtons);
        inlineKeyboardMarkup.setKeyboard(rowList);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(userSession.getChatId()));
        message.setText("Выберите действие из списка");
        message.setReplyMarkup(inlineKeyboardMarkup);
        return MessageResponse.builder()
                .messageType(SEND_MESSAGE)
                .dto(List.of(message))
                .build();
    }

    @Override
    public MessageResponse processCommand(UserSession userSession, String message) {
        if (commands.containsKey(message)) {
            return commands.get(message).apply(userSession);
        } else {
            return getDefaultResponse(userSession);
        }
    }

    private Function<UserSession, MessageResponse> getStevedores() {
        return userSession -> {
            userSession.setSessionState(SessionState.STEVEDORE_MENU);
            userSessionRepository.save(userSession);
            return null;
        };
    }

    private Function<UserSession, MessageResponse> getExporters() {
        return userSession -> {
            userSession.setSessionState(SessionState.EXPORTER_MENU);
            userSessionRepository.save(userSession);
            return null;
        };
    }

    private Function<UserSession, MessageResponse> getCrops() {
        return userSession -> {
            userSession.setSessionState(SessionState.CROP_MENU);
            userSessionRepository.save(userSession);
            return null;
        };
    }

    private Function<UserSession, MessageResponse> getTimeslots() {
        return userSession -> {
            userSession.setSessionState(SessionState.TIMESLOT_MENU);
            userSessionRepository.save(userSession);
            return null;
        };
    }

    private Function<UserSession, MessageResponse> getNotifications() {
        return userSession -> {
            userSession.setSessionState(SessionState.NOTIFICATION_MENU);
            userSessionRepository.save(userSession);
            return null;
        };
    }

}
