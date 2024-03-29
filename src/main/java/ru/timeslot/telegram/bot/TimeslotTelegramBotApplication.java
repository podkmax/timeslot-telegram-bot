package ru.timeslot.telegram.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.timeslot.telegram.bot.configuration.BotProperties;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties({
        BotProperties.class
})
public class TimeslotTelegramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeslotTelegramBotApplication.class, args);
    }

}
