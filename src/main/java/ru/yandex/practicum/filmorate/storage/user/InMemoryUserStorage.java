package ru.yandex.practicum.filmorate.storage.user;

import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@NoArgsConstructor
@Slf4j(topic = "TRACE")
@ConfigurationPropertiesScan
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Collection<User> findAll() {
        log.info("Обработка Get-запроса...");
        return users.values();
    }

    @Override
    public User findById(Long id) throws NotFoundException, ValidationException {
        log.info("Обработка Get-запроса...");
        if (id == null) {
            log.error(ExceptionMessages.ID_CANNOT_BE_NULL);
            throw new ValidationException(ExceptionMessages.ID_CANNOT_BE_NULL);
        }

        User user = users.get(id);
        if (user == null) {
            log.error(String.format("Пользователь с id %d не найден", id));
            throw new NotFoundException(String.format("Пользователь с id %d не найден", id));
        }
        return user;
    }

    @Override
    public User create(@Valid User user) throws ValidationException, DuplicatedDataException {
        log.info("Обработка Create-запроса...");
        duplicateCheck(user);

        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@") || user.getEmail().contains(" ")) {
            log.error(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY);
            throw new ValidationException(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY);
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.error(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY);
            throw new ValidationException(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY);
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        if (user.getBirthday() == null) {
            log.error(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL);
            throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL);
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE);
            throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE);
        }

        user.setId(getNextId());
        user.setFriends(new HashSet<>());
        users.put(user.getId(), user);
        return user;
    }

    private long getNextId() {
        long currentMaxId = users.keySet().stream().mapToLong(id -> id).max().orElse(0L);
        return ++currentMaxId;
    }

    private void duplicateCheck(User user) throws DuplicatedDataException {
        for (User existingUser : users.values()) {
            if (existingUser.getEmail().equals(user.getEmail())) {
                log.error(ExceptionMessages.EMAIL_ALREADY_EXISTS);
                throw new DuplicatedDataException(ExceptionMessages.EMAIL_ALREADY_EXISTS);
            }
        }
    }

    @Override
    public User update(@Valid User newUser) throws NotFoundException, ValidationException, DuplicatedDataException {
        log.info("Обработка Put-запроса...");
        if (newUser.getId() == null) {
            log.error(ExceptionMessages.ID_CANNOT_BE_NULL);
            throw new ValidationException(ExceptionMessages.ID_CANNOT_BE_NULL);
        }

        User oldUser = users.get(newUser.getId());
        if (oldUser == null) {
            log.error(String.format("Пользователь с id %d не найден", newUser.getId()));
            throw new NotFoundException(String.format("Пользователь с id %d не найден", newUser.getId()));
        }

        if (newUser.getEmail() == null || newUser.getEmail().isBlank() || !newUser.getEmail().contains("@") || newUser.getEmail().contains(" ")) {
            log.error(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY);
            throw new ValidationException(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY);
        }
        if (!newUser.getEmail().equals(oldUser.getEmail())) {
            duplicateCheck(newUser);
            oldUser.setEmail(newUser.getEmail());
        }

        if (newUser.getLogin() == null || newUser.getLogin().isBlank() || newUser.getLogin().contains(" ")) {
            log.error(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY);
            throw new ValidationException(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY);
        }
        oldUser.setLogin(newUser.getLogin());

        if (newUser.getName() == null || newUser.getName().isBlank()) {
            oldUser.setName(newUser.getLogin());
        } else {
            oldUser.setName(newUser.getName());
        }

        if (newUser.getBirthday() == null) {
            log.error(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL);
            throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL);
        }
        if (newUser.getBirthday().isAfter(LocalDate.now())) {
            log.error(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE);
            throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE);
        }
        oldUser.setBirthday(newUser.getBirthday());

        return oldUser;
    }
}