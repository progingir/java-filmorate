package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.*;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final Map<Long, User> users = new HashMap<>();

    // Возвращает всех пользователей
    public Collection<User> findAll() {
        log.info("Возвращаем список пользователей...");
        return users.values();
    }

    // Создает нового пользователя
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

    // Генерирует следующий ID для пользователя
    private long getNextId() {
        return users.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    // Проверка на дублирование email
    private void duplicateCheck(User user) {
        for (User u : users.values()) {
            if (u.getEmail().equals(user.getEmail())) {
                logAndThrow(new DuplicatedDataException("Пользователь с таким email уже существует"));
            }
        }
    }

    // Обновляет данные пользователя
    public User update(@Valid User newUser) {
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

    // Валидация email
    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@") || email.contains(" ") || email.length() == 1) {
            logAndThrow(new ValidationException("Некорректный email"));
        }
    }

    // Валидация логина
    private void validateLogin(String login) {
        if (login == null || login.contains(" ") || login.isBlank()) {
            logAndThrow(new ValidationException("Логин не может быть пустым или содержать пробелы"));
        }
    }

    // Валидация дня рождения
    private void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            logAndThrow(new ValidationException("Дата рождения не может быть null"));
        }
        if (birthday.isAfter(LocalDate.now())) {
            logAndThrow(new ValidationException("Дата рождения не может быть в будущем"));
        }
    }

    // Логирование и выброс исключения
    private void logAndThrow(RuntimeException exception) {
        log.error(exception.getMessage());
        throw exception;
    }

    // Добавление друга
    public void addFriend(Long userId, Long friendId) {
        log.info("Добавляем друга с ID = {} пользователю с ID = {}", friendId, userId);

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        // Добавляем друга в список друзей обоих пользователей
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        log.info("Пользователь с ID = {} теперь друг пользователя с ID = {}", friendId, userId);
    }

    // Удаление друга
    public void removeFriend(Long userId, Long friendId) {
        log.info("Удаляем друга с ID = {} у пользователя с ID = {}", friendId, userId);

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        // Удаляем друга из списка друзей обоих пользователей
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.info("Пользователь с ID = {} больше не является другом пользователя с ID = {}", friendId, userId);
    }

    // Получение списка общих друзей двух пользователей
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        log.info("Ищем общих друзей для пользователей с ID = {} и ID = {}", userId, otherUserId);

        User user = getUserById(userId);
        User otherUser = getUserById(otherUserId);

        // Находим пересечение множеств друзей
        Set<Long> commonFriendIds = new HashSet<>(user.getFriends());
        commonFriendIds.retainAll(otherUser.getFriends());

        // Преобразуем ID в объекты User
        return commonFriendIds.stream()
                .map(users::get)
                .collect(Collectors.toList());
    }

    // Вспомогательный метод для получения пользователя по ID
    private User getUserById(Long id) {
        if (id == null) {
            logAndThrow(new ValidationException("ID не может быть null"));
        }
        User user = users.get(id);
        if (user == null) {
            logAndThrow(new NotFoundException("Пользователь с ID = " + id + " не найден"));
        }
        return user;
    }
}