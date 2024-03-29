package ru.timeslot.telegram.bot.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;


public class TimeslotEvent extends ApplicationEvent {
    private String message;
    private Map<Long, Long> userWithChatId;
    public TimeslotEvent(Object source, String message, Map<Long, Long> userWithChatId) {
        super(source);
        this.message = message;
        this.userWithChatId = userWithChatId;
    }

    public String getMessage() {
        return message;
    }

    public Map<Long, Long> getUserWithChatId() {
        return userWithChatId;
    }
}
