package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) throws ValidationException, DuplicatedDataException {
        return userStorage.create(user);
    }

    public User update(User user) throws NotFoundException, ValidationException {
        return userStorage.update(user);
    }

    public User findById(Long id) throws NotFoundException, ValidationException {
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

    public Collection<User> getCommonFriends(Long userId, Long otherUserId) throws NotFoundException {
        return userStorage.getCommonFriends(userId, otherUserId);
    }
}