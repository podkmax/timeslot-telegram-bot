package ru.timeslot.telegram.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.timeslot.telegram.bot.domain.User;
import ru.timeslot.telegram.bot.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final List<Long> activeUsers = new ArrayList<>();

    private final UserRepository userRepository;
    public boolean checkUserAccess(Long userTelegramId) {
        if (activeUsers.contains(userTelegramId)) {
            return true;
        } else {
            Optional<User> optionalUser = userRepository.findByUserIdAndActiveIsTrue(userTelegramId);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                activeUsers.add(user.getUserId());
                return true;
            } else {
                return false;
            }
        }
    }
}
