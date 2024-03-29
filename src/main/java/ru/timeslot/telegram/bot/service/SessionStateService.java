package ru.timeslot.telegram.bot.service;

import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;

public interface SessionStateService {
    /**
     * Стандартный ответ который отправится пользователю при обработке несуществующей команды
     *
     * @param userSession@return сообщение пользователю
     */
    MessageResponse getDefaultResponse(UserSession userSession);
    MessageResponse processCommand(UserSession userSession, String message);
}
