package ru.timeslot.telegram.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.timeslot.telegram.bot.domain.User;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserIdAndActiveIsTrue(Long userTelegramId);

    @Query(
            value = "select * from user_table ut where ut.subscribe = true",
            nativeQuery = true
    )
    List<User> findWithActiveSubscribe();

    Optional<User> findByUserId(Long userId);
}
