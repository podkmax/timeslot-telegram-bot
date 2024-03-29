package ru.timeslot.telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.timeslot.parser.dto.StevedoreDto;
import ru.timeslot.telegram.bot.client.TimeslotParserClient;
import ru.timeslot.telegram.bot.exception.StevedoreUpdateException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StevedoreService {
    private final TimeslotParserClient timeslotParserClient;

    //TODO Добавить ретраи
    public Map<Integer, StevedoreDto> getStevedores() {
        try {
            //TODO добавить кофеин для кэша
            return timeslotParserClient.getAllStevedores().stream()
                    .collect(Collectors.toMap(StevedoreDto::getId, Function.identity()));
        } catch (Exception e) {
            log.error("Ошибка получения стивидоров", e);
            throw new StevedoreUpdateException("Ошибка получения стивидоров");
        }
    }
}
