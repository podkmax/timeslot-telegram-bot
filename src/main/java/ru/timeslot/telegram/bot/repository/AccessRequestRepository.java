package ru.timeslot.telegram.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.timeslot.telegram.bot.domain.AccessRequest;

import java.util.Optional;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Integer> {
    Optional<AccessRequest> findByUserId(Long userId);
}
