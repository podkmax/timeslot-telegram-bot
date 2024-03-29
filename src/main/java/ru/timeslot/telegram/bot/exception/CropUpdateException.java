package ru.timeslot.telegram.bot.exception;

public class CropUpdateException extends RuntimeException {
    public CropUpdateException(String message) {
        super(message);
    }
}
