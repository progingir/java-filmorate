package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserService {

    private final Map<Long, User> users = new HashMap<>();

    public Collection<User> findAll() {
        log.info("Возвращаем список пользователей...");
        return users.values();
    }

    public User create(@Valid User user) {
        log.info("Создаем нового пользователя...");
        duplicateCheck(user);

        validateEmail(user.getEmail());
        validateLogin(user.getLogin());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        validateBirthday(user.getBirthday());

        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    private long getNextId() {
        return users.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    private void duplicateCheck(User user) {
        for (User u : users.values()) {
            if (u.getEmail().equals(user.getEmail())) {
                logAndThrow(new DuplicatedDataException(ExceptionMessages.EMAIL_ALREADY_EXISTS));
            }
        }
    }

    public User update(@Valid User newUser) {
        if (newUser.getId() == null) {
            logAndThrow(new ValidationException(ExceptionMessages.ID_CANNOT_BE_NULL));
        }

        if (users.containsKey(newUser.getId())) {
            User oldUser = users.get(newUser.getId());

            oldUser.setEmail(newUser.getEmail());
            oldUser.setLogin(newUser.getLogin());
            oldUser.setName(newUser.getName() != null ? newUser.getName() : newUser.getLogin());
            oldUser.setBirthday(newUser.getBirthday());
            return oldUser;
        } else {
            logAndThrow(new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден"));
        }
        return null;
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@") || email.contains(" ") || email.length() == 1) {
            logAndThrow(new ValidationException(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY));
        }
    }

    private void validateLogin(String login) {
        if (login == null || login.contains(" ") || login.isBlank()) {
            logAndThrow(new ValidationException(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY));
        }
    }

    private void validateBirthday(LocalDate birthday) {
        if (birthday != null) {
            if (birthday.isAfter(LocalDate.now())) {
                logAndThrow(new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE));
            }
        } else {
            logAndThrow(new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL));
        }
    }

    private void logAndThrow(RuntimeException exception) {
        log.error(exception.getMessage());
        throw exception;
    }
}