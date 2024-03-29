package ru.timeslot.telegram.bot.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@ConfigurationProperties(prefix = "bot")
public record BotProperties(String botName, String botToken) {
}
