package ru.timeslot.telegram.bot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.timeslot.telegram.bot.bot.TimeslotTelegramBot;

@Configuration
public class TimeslotTelegramBotConfiguration {
    @Bean
    public TelegramBotsApi telegramBotsApi(TimeslotTelegramBot timeslotBot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(timeslotBot);
        return api;
    }
}
