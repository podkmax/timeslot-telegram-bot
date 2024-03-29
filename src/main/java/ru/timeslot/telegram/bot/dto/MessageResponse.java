package ru.timeslot.telegram.bot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessageResponse {
    private ResponseMessageType messageType;
    private List<Object> dto;
}
