package ru.timeslot.telegram.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.timeslot.telegram.bot.domain.UserSession;

import java.util.Optional;


@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {
    Optional<UserSession> findByUserId(Long userId);
}
