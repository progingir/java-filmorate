package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

public interface UserStorage {

    // Получение всех пользователей
    Collection<User> findAll();

    // Создание нового пользователя
    User create(User user);

    // Обновление существующего пользователя
    User update(User user) throws NotFoundException;

    // Добавление друга
    void addFriend(Long userId, Long friendId) throws NotFoundException;

    // Удаление друга
    void removeFriend(Long userId, Long friendId) throws NotFoundException;

    // Получение общих друзей двух пользователей
    Collection<User> getCommonFriends(Long userId, Long otherUserId) throws NotFoundException;

    // Получение пользователя по ID
    User findById(Long id) throws NotFoundException;

    // Получение списка друзей пользователя
    Collection<User> getFriends(Long id) throws NotFoundException;
}