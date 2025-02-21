package ru.yandex.practicum.filmorate.storage.user;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.*;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();

    @Override
    public Collection<User> findAll() {
        log.info("Возвращаем список пользователей...");
        return users.values();
    }

    @Override
    public User create(@Valid User user) {
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

    @Override
    public User update(@Valid User newUser) throws NotFoundException {
        if (newUser.getId() == null) {
            logAndThrow(new ValidationException("ID пользователя не может быть null"));
        }
        if (users.containsKey(newUser.getId())) {
            User oldUser = users.get(newUser.getId());
            oldUser.setEmail(newUser.getEmail());
            oldUser.setLogin(newUser.getLogin());
            oldUser.setName(newUser.getName() != null ? newUser.getName() : newUser.getLogin());
            oldUser.setBirthday(newUser.getBirthday());
            return oldUser;
        } else {
            logAndThrow(new NotFoundException("Пользователь с ID = " + newUser.getId() + " не найден"));
        }
        return null;
    }

    public User findById(Long id) throws NotFoundException {
        if (id == null) {
            logAndThrow(new ValidationException("ID не может быть null"));
        }
        User user = users.get(id);
        if (user == null) {
            logAndThrow(new NotFoundException("Пользователь с ID = " + id + " не найден"));
        }
        return user;
    }

    @Override
    public void addFriend(Long userId, Long friendId) throws NotFoundException {
        User user = findById(userId);
        User friend = findById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("Пользователь с ID = {} добавлен в друзья к пользователю с ID = {}", friendId, userId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) throws NotFoundException {
        User user = findById(userId);
        User friend = findById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        log.info("Пользователь с ID = {} удален из друзей пользователя с ID = {}", friendId, userId);
    }

    public Collection<User> getFriends(Long id) throws NotFoundException {
        User user = findById(id);
        return user.getFriends().stream()
                .map(users::get)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherUserId) throws NotFoundException {
        User user = findById(userId);
        User otherUser = findById(otherUserId);
        Set<Long> commonFriendIds = new HashSet<>(user.getFriends());
        commonFriendIds.retainAll(otherUser.getFriends());
        return commonFriendIds.stream()
                .map(users::get)
                .collect(Collectors.toList());
    }

    private long getNextId() {
        return users.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    private void duplicateCheck(User user) {
        for (User u : users.values()) {
            if (u.getEmail().equals(user.getEmail())) {
                logAndThrow(new DuplicatedDataException("Пользователь с таким email уже существует"));
            }
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@") || email.contains(" ") || email.length() == 1) {
            logAndThrow(new ValidationException("Некорректный email"));
        }
    }

    private void validateLogin(String login) {
        if (login == null || login.contains(" ") || login.isBlank()) {
            logAndThrow(new ValidationException("Логин не может быть пустым или содержать пробелы"));
        }
    }

    private void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            logAndThrow(new ValidationException("Дата рождения не может быть null"));
        }
        if (birthday.isAfter(LocalDate.now())) {
            logAndThrow(new ValidationException("Дата рождения не может быть в будущем"));
        }
    }

    private void logAndThrow(RuntimeException exception) {
        log.error(exception.getMessage());
        throw exception;
    }
}