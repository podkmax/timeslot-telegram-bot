package ru.timeslot.telegram.bot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timeslot.parser.dto.TimeslotDto;
import ru.timeslot.telegram.bot.service.TimeslotService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/timeslot")
public class TimeslotController {

    private final TimeslotService timeslotService;

    @PostMapping("/new")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notifyNewTimeslot(@RequestBody TimeslotDto timeslotDto) {
        timeslotService.notifyNewTimeslot(timeslotDto);
    }
}
