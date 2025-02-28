package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Set;

public interface UserInterface {
    User addFriend(Long idUser, Long idFriend);

    User delFriend(Long idUser, Long idFriend);

    Set<User> findJointFriends(Long idUser, Long idFriend);

    Set<User> findAllFriends(Long idUser);
}