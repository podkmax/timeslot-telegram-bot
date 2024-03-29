package ru.timeslot.telegram.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.timeslot.parser.dto.TimeslotDto;
import ru.timeslot.telegram.bot.client.TimeslotParserClient;
import ru.timeslot.telegram.bot.domain.User;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.event.TimeslotEvent;
import ru.timeslot.telegram.bot.repository.UserRepository;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeslotService {
    private final TimeslotParserClient timeslotParserClient;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public List<TimeslotDto> getAllTimeslots() {
        return timeslotParserClient.getAllTimeslots();
    }

    public void notifyNewTimeslot(TimeslotDto timeslotDto) {
        List<User> withActiveSubscribe = userRepository.findWithActiveSubscribe();
        Map<Long, Long> userWithChatId = withActiveSubscribe.stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        u -> userSessionRepository.findByUserId(
                                u.getUserId()).map(UserSession::getChatId).orElseGet(null)
                        )
                );
        String message = String.format(
                "Стивидор: %s, Экспортер: %s, Культура: %s, Дата: %s",
                timeslotDto.getStevedore().getName(),
                timeslotDto.getExporter().getName(),
                timeslotDto.getCrop().getName(),
                timeslotDto.getTimeslotDate()
        );
        TimeslotEvent event = new TimeslotEvent(this, message, userWithChatId);
        applicationEventPublisher.publishEvent(event);
    }
}
