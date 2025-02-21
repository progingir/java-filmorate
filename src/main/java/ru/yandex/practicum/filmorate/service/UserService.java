package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        log.info("Возвращаем список пользователей...");
        return userStorage.findAll();
    }

    public User create(User user) {
        log.info("Создаем нового пользователя...");
        return userStorage.create(user);
    }

    public User update(User user) throws NotFoundException {
        log.info("Обновляем пользователя...");
        return userStorage.update(user);
    }

    public User getUserById(Long id) throws NotFoundException {
        return userStorage.findById(id);
    }

    public void addFriend(Long userId, Long friendId) throws NotFoundException {
        userStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) throws NotFoundException {
        userStorage.removeFriend(userId, friendId);
    }

    public Collection<User> getFriends(Long id) throws NotFoundException {
        return userStorage.getFriends(id);
    }

    public Collection<User> getCommonFriends(Long id, Long otherId) throws NotFoundException {
        return userStorage.getCommonFriends(id, otherId);
    }
}