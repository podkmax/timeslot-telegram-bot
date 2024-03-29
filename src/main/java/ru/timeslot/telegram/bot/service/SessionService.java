package ru.timeslot.telegram.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final AuthService authService;

    public UserSession getOrCreateUserSession(Long userId, Long chatId, Integer messageId, String message) {
        Optional<UserSession> optionalUserSession = userSessionRepository.findByUserId(userId);
        if (optionalUserSession.isEmpty()) {
            if (authService.checkUserAccess(userId)) {
                UserSession session = createSession(userId, chatId, messageId, SessionState.MAIN_MENU);
                return userSessionRepository.save(session);
            } else {
                UserSession session = createSession(userId, chatId, messageId, SessionState.ACCESS_DENIED);
                return userSessionRepository.save(session);
            }
        } else {
            UserSession userSession = optionalUserSession.get();
            userSession.setLastMessageId(messageId);
            userSession.setLastMessageText(message);
            return userSessionRepository.save(userSession);
        }
    }

    private UserSession createSession(Long userId, Long chatId, Integer messageId, SessionState state) {
        return UserSession.builder()
                .createdDateTime(Instant.now())
                .expiredDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .userId(userId)
                .chatId(chatId)
                .lastMessageId(messageId)
                .sessionState(state)
                .build();
    }
}
