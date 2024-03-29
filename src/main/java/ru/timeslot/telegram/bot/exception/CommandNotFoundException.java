package ru.timeslot.telegram.bot.exception;

public class CommandNotFoundException extends RuntimeException {
    public CommandNotFoundException(String message) {
        super("Команда " + message + " не найдена");
    }
}
