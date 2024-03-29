package ru.timeslot.telegram.bot.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.service.SessionStateService;
import ru.timeslot.telegram.bot.service.state.AccessDeniedStateService;
import ru.timeslot.telegram.bot.service.state.CropMenuStateService;
import ru.timeslot.telegram.bot.service.state.ExporterMenuStateService;
import ru.timeslot.telegram.bot.service.state.MainMenuStateService;
import ru.timeslot.telegram.bot.service.state.NotificationMenuStateService;
import ru.timeslot.telegram.bot.service.state.StevedoreMenuStateService;
import ru.timeslot.telegram.bot.service.state.TimeSlotMenuStateService;

import java.util.Map;

import static ru.timeslot.telegram.bot.domain.SessionState.ACCESS_DENIED;
import static ru.timeslot.telegram.bot.domain.SessionState.CROP_MENU;
import static ru.timeslot.telegram.bot.domain.SessionState.EXPORTER_MENU;
import static ru.timeslot.telegram.bot.domain.SessionState.MAIN_MENU;
import static ru.timeslot.telegram.bot.domain.SessionState.NOTIFICATION_MENU;
import static ru.timeslot.telegram.bot.domain.SessionState.STEVEDORE_MENU;
import static ru.timeslot.telegram.bot.domain.SessionState.TIMESLOT_MENU;

@Configuration
@RequiredArgsConstructor
public class HandlersConfiguration {

    private final AccessDeniedStateService accessDeniedStateService;
    private final MainMenuStateService mainMenuStateService;
    private final StevedoreMenuStateService stevedoreMenuStateService;
    private final CropMenuStateService cropMenuStateService;
    private final ExporterMenuStateService exporterMenuStateService;
    private final TimeSlotMenuStateService timeSlotMenuStateService;
    private final NotificationMenuStateService notificationMenuStateService;

    @Bean
    public Map<SessionState, SessionStateService> stateHandlers() {
        return Map.of(
                ACCESS_DENIED, accessDeniedStateService,
                MAIN_MENU, mainMenuStateService,
                STEVEDORE_MENU, stevedoreMenuStateService,
                CROP_MENU, cropMenuStateService,
                EXPORTER_MENU, exporterMenuStateService,
                TIMESLOT_MENU, timeSlotMenuStateService,
                NOTIFICATION_MENU, notificationMenuStateService
        );
    }
}
