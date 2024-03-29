package ru.timeslot.telegram.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.timeslot.parser.dto.CropDto;
import ru.timeslot.parser.dto.StevedoreCropIdDto;
import ru.timeslot.telegram.bot.client.TimeslotParserClient;
import ru.timeslot.telegram.bot.exception.CropUpdateException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CropService {
    private final TimeslotParserClient timeslotParserClient;

    public void toggleCrop(StevedoreCropIdDto stevedoreCropIdDto) {
        timeslotParserClient.toggleCrop(stevedoreCropIdDto);
    }

    public List<CropDto> getCrops() {
        try {
            //TODO добавить кофеин для кэша
            return timeslotParserClient.getAllCrops();
        } catch (Exception e) {
            log.error("Ошибка получения стивидоров", e);
            throw new CropUpdateException("Ошибка получения стивидоров");
        }
    }
}
