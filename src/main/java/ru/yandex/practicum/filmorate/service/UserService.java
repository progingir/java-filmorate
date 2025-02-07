package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
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


    public User findById(String id) throws ValidationException {
        log.info("Находим пользователя по айди...");
        if (id.isBlank()) {
            log.error("Exception", new ValidationException(ExceptionMessages.USER_ID_CANNOT_BE_NULL));
            throw new ValidationException(ExceptionMessages.USER_ID_CANNOT_BE_NULL);
        } else for (User u : users.values())
            if (u.getId().equals(Long.valueOf(id))) {
                return u;
            }
        log.error("Exception", new ValidationException(ExceptionMessages.USER_NOT_FOUND));
        throw new ValidationException(ExceptionMessages.USER_NOT_FOUND);
    }


    public User create(@Valid User user) throws ValidationException, DuplicatedDataException {
        log.info("Создаем нового пользователя...");
        duplicateCheck(user);
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@") ||
                user.getEmail().contains(" ") || user.getEmail().length() == 1) {
            log.error("Exception", new ValidationException(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY));
            throw new ValidationException(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY);
        }
        if (user.getLogin() == null || user.getLogin().contains(" ") || user.getLogin().isBlank()) {
            log.error("Exception", new ValidationException(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY));
            throw new ValidationException(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY);
        }
        if (user.getName() == null || user.getName().isBlank()) user.setName(user.getLogin());
        if (user.getBirthday() != null) {
            if (user.getBirthday().isAfter(LocalDate.now())) {
                log.error("Exception", new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE));
                throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE);
            }
        } else {
            log.error("Exception", new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL));
            throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL);
        }
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }


    private long getNextId() {
        long currentMaxId = users.keySet().stream().mapToLong(id -> id).max().orElse(0);
        return ++currentMaxId;
    }


    private void duplicateCheck(User user) throws DuplicatedDataException {
        for (User u : users.values())
            if (u.getEmail().equals(user.getEmail())) {
                log.error("Exception", new DuplicatedDataException(ExceptionMessages.EMAIL_ALREADY_EXISTS));
                throw new DuplicatedDataException(ExceptionMessages.EMAIL_ALREADY_EXISTS);
            }
    }


    public User update(@Valid User newUser) throws ValidationException, NotFoundException, DuplicatedDataException {
        if (newUser.getId() == null) {
            log.error("Exception", new ValidationException(ExceptionMessages.ID_CANNOT_BE_NULL));
            throw new ValidationException(ExceptionMessages.ID_CANNOT_BE_NULL);
        }
        if (users.containsKey(newUser.getId())) {
            User oldUser = users.get(newUser.getId());
            if (newUser.getEmail() == null || newUser.getEmail().isBlank() || !newUser.getEmail().contains("@") ||
                    newUser.getEmail().contains(" ") || newUser.getEmail().length() == 1) {
                log.error("Exception", new ValidationException(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY));
                throw new ValidationException(ExceptionMessages.EMAIL_CANNOT_BE_EMPTY);
            } else if (!newUser.getEmail().equals(oldUser.getEmail())) {
                for (User u : users.values())
                    if (u.getEmail().equals(newUser.getEmail())) {
                        log.error("Exception", new DuplicatedDataException(ExceptionMessages.EMAIL_ALREADY_EXISTS));
                        throw new DuplicatedDataException(ExceptionMessages.EMAIL_ALREADY_EXISTS);
                    }
                oldUser.setEmail(newUser.getEmail());
            }
            if (newUser.getLogin() == null || newUser.getLogin().contains(" ") || newUser.getLogin().isBlank()) {
                log.error("Exception", new ValidationException(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY));
                throw new ValidationException(ExceptionMessages.LOGIN_CANNOT_BE_EMPTY);
            } else oldUser.setLogin(newUser.getLogin());
            if (newUser.getName() == null || newUser.getName().isBlank()) oldUser.setName(newUser.getLogin());
            else oldUser.setName(newUser.getName());
            if (newUser.getBirthday() != null) {
                if (newUser.getBirthday().isAfter(LocalDate.now())) {
                    log.error("Exception", new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE));
                    throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_IN_FUTURE);
                } else oldUser.setBirthday(newUser.getBirthday());
            } else {
                log.error("Exception", new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL));
                throw new ValidationException(ExceptionMessages.BIRTHDAY_CANNOT_BE_NULL);
            }
            return oldUser;
        } else {
            log.error("Exception", new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден"));
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }
    }
}