package ru.timeslot.telegram.bot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.timeslot.parser.dto.CropDto;
import ru.timeslot.parser.dto.ExporterDto;
import ru.timeslot.parser.dto.StevedoreCropIdDto;
import ru.timeslot.parser.dto.StevedoreDto;
import ru.timeslot.parser.dto.StevedoreExporterIdDto;
import ru.timeslot.parser.dto.TimeslotDto;

import java.util.List;


@FeignClient(
        value = "timeslot-parser-client",
        url = "${client.timeslot-parser.url}",
        configuration = FeignConfiguration.class
)
public interface TimeslotParserClient {

    @GetMapping("/stevedore/all")
    List<StevedoreDto> getAllStevedores();

    @PutMapping("/stevedore/toggle/{id}")
    StevedoreDto toggleStevedore(@PathVariable Integer id);

    @GetMapping("/crop/all")
    List<CropDto> getAllCrops();

    @PutMapping("/crop/toggle/active")
    void toggleCrop(@RequestBody StevedoreCropIdDto stevedoreCropIdDto);

    @GetMapping("/exporter/all")
    List<ExporterDto> getAllExporters();

    @PutMapping("/exporter/toggle/active")
    void toggleExporter(@RequestBody StevedoreExporterIdDto stevedoreExporterIdDto);

    @GetMapping("/timeslot/all")
    List<TimeslotDto> getAllTimeslots();
}
