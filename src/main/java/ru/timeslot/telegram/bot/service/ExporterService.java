package ru.timeslot.telegram.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.timeslot.parser.dto.ExporterDto;
import ru.timeslot.parser.dto.StevedoreExporterIdDto;
import ru.timeslot.telegram.bot.client.TimeslotParserClient;
import ru.timeslot.telegram.bot.exception.StevedoreUpdateException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExporterService {
    private final TimeslotParserClient timeslotParserClient;

    public void toggleExporters(StevedoreExporterIdDto stevedoreExporterIdDto) {
        timeslotParserClient.toggleExporter(stevedoreExporterIdDto);
    }

    public List<ExporterDto> getExporters() {
        try {
            //TODO добавить кофеин для кэша
            return timeslotParserClient.getAllExporters();
        } catch (Exception e) {
            log.error("Ошибка получения стивидоров", e);
            throw new StevedoreUpdateException("Ошибка получения стивидоров");
        }
    }
}
