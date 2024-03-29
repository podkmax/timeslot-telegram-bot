package ru.timeslot.telegram.bot.service.state;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.timeslot.parser.dto.ExporterDto;
import ru.timeslot.parser.dto.StevedoreExporterIdDto;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.exception.CommandNotFoundException;
import ru.timeslot.telegram.bot.exception.StevedoreUpdateException;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;
import ru.timeslot.telegram.bot.service.ExporterService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;
import static ru.timeslot.telegram.bot.dto.ResponseMessageType.EDIT_MESSAGE;
import static ru.timeslot.telegram.bot.dto.ResponseMessageType.SEND_MESSAGE;
import static ru.timeslot.telegram.bot.dto.TextConstant.BACK_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.GET_EXPORTERS_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.GREEN;
import static ru.timeslot.telegram.bot.dto.TextConstant.RED;
import static ru.timeslot.telegram.bot.dto.TextConstant.TOGGLE_COMMAND;

@Service
@RequiredArgsConstructor
public class ExporterMenuStateService extends AbstractMenuStateService {

    private final ExporterService exporterService;
    private final UserSessionRepository userSessionRepository;
    private final Map<String, Function<UserSession, MessageResponse>> commands = Map.of(
            GET_EXPORTERS_COMMAND, getExportersButtons(),
            TOGGLE_COMMAND, toggleExporters(),
            BACK_COMMAND, goBack()
    );

    @Override
    public MessageResponse getDefaultResponse(UserSession userSession) {
        return getSimpleMessage(
                userSession.getChatId(),
                String.format("Команда «%s» не распознана. Нажмите одну из представленных кнопок",  userSession.getLastMessageText())
        );
    }

    @Override
    public MessageResponse processCommand(UserSession userSession, String message) {
        try {
            String command = extractCommand(message);
            if (commands.containsKey(command)) {
                return commands.get(command).apply(userSession);
            }
        } catch (StevedoreUpdateException e) {
            return getSimpleMessage(
                    userSession.getChatId(),"Ошибка получения списка доступных Экспортеров. Сервер недоступен."
            );
        } catch (CommandNotFoundException e) {
            return getDefaultResponse(userSession);
        }
        return null;
    }

    private Function<UserSession, MessageResponse> getExportersButtons() {
        return userSession -> {
            List<ExporterDto> allCrops = exporterService.getExporters();
            List<Object> messages = new ArrayList<>();

            Map<Integer, List<ExporterDto>> exporterByStevedore = allCrops.stream().collect(Collectors.groupingBy(ExporterDto::getStevedoreId));
            for ( Map.Entry<Integer, List<ExporterDto>> entry : exporterByStevedore.entrySet()) {
                InlineKeyboardMarkup stevedoreKeyboard = getCropKeyboard(entry.getValue());
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(userSession.getChatId()));
                message.setText("Список экспортеров для Стивидора: " + entry.getValue().getFirst().getStevedoreName());
                message.setReplyMarkup(stevedoreKeyboard);
                messages.add(message);
            }

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(userSession.getChatId()));
            message.setText("Вернуться в предыдущее меню");
            message.setReplyMarkup(getBackKeyboard());
            messages.add(message);

            return MessageResponse.builder()
                    .messageType(SEND_MESSAGE)
                    .dto(messages)
                    .build();
        };
    }

    private Function<UserSession, MessageResponse> toggleExporters() {
        return userSession -> {
            Integer stevedoreId = extractStevedoreId(userSession.getLastMessageText());

            Map<Integer, List<ExporterDto>> exportersByStevedoreId = exporterService.getExporters().stream()
                    .collect(Collectors.groupingBy(ExporterDto::getStevedoreId));

            Map<Integer, ExporterDto> exporterById = exportersByStevedoreId.get(stevedoreId).stream()
                    .collect(Collectors.toMap(ExporterDto::getId, Function.identity()));

            List<ExporterDto> exporterList = exportersByStevedoreId.get(stevedoreId);
            InlineKeyboardMarkup exporterKeyboard = getCropKeyboard(exporterList);
            exporterKeyboard.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .filter(b -> b.getCallbackData().equals(userSession.getLastMessageText()))
                    .forEach(b -> {
                        ExporterDto exporterDto = exporterById.get(extractCropId(b.getCallbackData()));
                        b.setText(
                                Boolean.TRUE.equals(exporterDto.getActive())
                                        ? RED + exporterDto.getName()
                                        : GREEN + exporterDto.getName()
                        );
                        StevedoreExporterIdDto stevedoreExporterIdDto = new StevedoreExporterIdDto(exporterDto.getStevedoreId(), exporterDto.getId());
                        exporterService.toggleExporters(stevedoreExporterIdDto);
                        exporterDto.setActive(!exporterDto.getActive());
                    });
            EditMessageText editedMessage = new EditMessageText();
            editedMessage.setChatId(userSession.getChatId());
            editedMessage.setMessageId(toIntExact(userSession.getLastMessageId()));
            editedMessage.setReplyMarkup(exporterKeyboard);
            editedMessage.setText("Список культур для Стивидора: " + exporterList.getFirst().getStevedoreName());
            return MessageResponse.builder()
                    .messageType(EDIT_MESSAGE)
                    .dto(List.of(editedMessage))
                    .build();
        };
    }

    private Function<UserSession, MessageResponse> goBack() {
        return session -> {
            session.setSessionState(SessionState.MAIN_MENU);
            userSessionRepository.save(session);
            return null;
        };
    }

    private InlineKeyboardMarkup getCropKeyboard(List<ExporterDto> exporters) {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
            for (ExporterDto exporterDto : exporters) {
                List<InlineKeyboardButton> exporterButtons = new ArrayList<>();
                InlineKeyboardButton exporterButton = new InlineKeyboardButton();
                exporterButton.setText(
                        Boolean.TRUE.equals(exporterDto.getActive())
                                ? String.format("%s %s", GREEN, exporterDto.getName())
                                : String.format("%s %s", RED, exporterDto.getName())
                );
                exporterButton.setCallbackData(
                        String.format("/toggle/activate/%s/%s", exporterDto.getStevedoreId(), exporterDto.getId())
                );
                exporterButtons.add(exporterButton);
                rowList.add(exporterButtons);
            }
            inlineKeyboardMarkup.setKeyboard(rowList);
            return inlineKeyboardMarkup;

    }

    private InlineKeyboardMarkup getBackKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> backLine = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData("/back");
        backLine.add(backButton);
        rowList.add(backLine);
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;

    }

    private Integer extractStevedoreId(String message) {
        List<String> splitCommands = Arrays.asList(message.split("/"));
        return Integer.parseInt(splitCommands.get(3));
    }

    private Integer extractCropId(String message) {
        List<String> splitCommands = Arrays.asList(message.split("/"));
        return Integer.parseInt(splitCommands.get(4));
    }
}
