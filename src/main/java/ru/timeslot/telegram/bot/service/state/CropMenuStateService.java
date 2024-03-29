package ru.timeslot.telegram.bot.service.state;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.timeslot.parser.dto.CropDto;
import ru.timeslot.parser.dto.StevedoreCropIdDto;
import ru.timeslot.telegram.bot.domain.SessionState;
import ru.timeslot.telegram.bot.domain.UserSession;
import ru.timeslot.telegram.bot.dto.MessageResponse;
import ru.timeslot.telegram.bot.exception.CommandNotFoundException;
import ru.timeslot.telegram.bot.exception.CropUpdateException;
import ru.timeslot.telegram.bot.repository.UserSessionRepository;
import ru.timeslot.telegram.bot.service.CropService;

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
import static ru.timeslot.telegram.bot.dto.TextConstant.GET_CROPS_COMMAND;
import static ru.timeslot.telegram.bot.dto.TextConstant.GREEN;
import static ru.timeslot.telegram.bot.dto.TextConstant.RED;
import static ru.timeslot.telegram.bot.dto.TextConstant.TOGGLE_COMMAND;

@Service
@RequiredArgsConstructor
public class CropMenuStateService extends AbstractMenuStateService {

    private final CropService cropService;
    private final UserSessionRepository userSessionRepository;
    private final Map<String, Function<UserSession, MessageResponse>> commands = Map.of(
            GET_CROPS_COMMAND, getCropButtons(),
            TOGGLE_COMMAND, toggleStevedore(),
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
        } catch (CropUpdateException e) {
            return getSimpleMessage(
                    userSession.getChatId(),"Ошибка получения списка доступных Культур. Сервер недоступен."
            );
        } catch (CommandNotFoundException e) {
            return getDefaultResponse(userSession);
        }
        return null;
    }

    private Function<UserSession, MessageResponse> getCropButtons() {
        return userSession -> {
            List<CropDto> allCrops = cropService.getCrops();
            List<Object> messages = new ArrayList<>();

            Map<Integer, List<CropDto>> cropByStevedore = allCrops.stream().collect(Collectors.groupingBy(CropDto::getStevedoreId));
            for ( Map.Entry<Integer, List<CropDto>> entry : cropByStevedore.entrySet()) {
                InlineKeyboardMarkup stevedoreKeyboard = getCropKeyboard(entry.getValue());
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(userSession.getChatId()));
                message.setText("Список культур для Стивидора: " + entry.getValue().getFirst().getStevedoreName());
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

    private Function<UserSession, MessageResponse> toggleStevedore() {
        return userSession -> {
            Integer stevedoreId = extractStevedoreId(userSession.getLastMessageText());

            Map<Integer, List<CropDto>> cropsByStevedoreId = cropService.getCrops().stream()
                    .collect(Collectors.groupingBy(CropDto::getStevedoreId));

            Map<Integer, CropDto> cropById = cropsByStevedoreId.get(stevedoreId).stream()
                    .collect(Collectors.toMap(CropDto::getId, Function.identity()));

            List<CropDto> cropList = cropsByStevedoreId.get(stevedoreId);
            InlineKeyboardMarkup cropKeyboard = getCropKeyboard(cropList);
            cropKeyboard.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .filter(b -> b.getCallbackData().equals(userSession.getLastMessageText()))
                    .forEach(b -> {
                        CropDto cropDto = cropById.get(extractCropId(b.getCallbackData()));
                        b.setText(
                                Boolean.TRUE.equals(cropDto.getActive())
                                        ? RED + cropDto.getName()
                                        : GREEN + cropDto.getName()
                        );
                        StevedoreCropIdDto stevedoreCropIdDto = new StevedoreCropIdDto(cropDto.getStevedoreId(), cropDto.getId());
                        cropService.toggleCrop(stevedoreCropIdDto);
                        cropDto.setActive(!cropDto.getActive());
                    });
            EditMessageText editedMessage = new EditMessageText();
            editedMessage.setChatId(userSession.getChatId());
            editedMessage.setMessageId(toIntExact(userSession.getLastMessageId()));
            editedMessage.setReplyMarkup(cropKeyboard);
            editedMessage.setText("Список культур для Стивидора: " + cropList.getFirst().getStevedoreName());
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

    private InlineKeyboardMarkup getCropKeyboard(List<CropDto> crops) {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
            for (CropDto cropDto : crops) {
                List<InlineKeyboardButton> cropButtons = new ArrayList<>();
                InlineKeyboardButton cropButton = new InlineKeyboardButton();
                cropButton.setText(
                        Boolean.TRUE.equals(cropDto.getActive())
                                ? String.format("%s %s", GREEN, cropDto.getName())
                                : String.format("%s %s", RED, cropDto.getName())
                );
                cropButton.setCallbackData(
                        String.format("/toggle/activate/%s/%s", cropDto.getStevedoreId(), cropDto.getId())
                );
                cropButtons.add(cropButton);
                rowList.add(cropButtons);
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
