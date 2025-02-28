package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserInterface;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Set;

@RestController
@RequestMapping("/users")

public class UserController {

    private final UserStorage userStorage;
    private final UserInterface userInterface;

    @Autowired
    public UserController(UserStorage userStorage, UserInterface userInterface) {
        this.userStorage = userStorage;
        this.userInterface = userInterface;
    }

    @GetMapping
    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable("id") Long id) throws ConditionsNotMetException {
        return userStorage.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody User user) throws ConditionsNotMetException, DuplicatedDataException {
        return userStorage.create(user);
    }

    @PutMapping
    public User update(@Valid @RequestBody User newUser) throws ConditionsNotMetException, NotFoundException, DuplicatedDataException {
        return userStorage.update(newUser);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public User addFriend(@Valid @RequestBody @PathVariable("id") Long id, @PathVariable("friendId") Long friendId) throws ConditionsNotMetException, NotFoundException, DuplicatedDataException {
        return userInterface.addFriend(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public User delFriend(@Valid @RequestBody @PathVariable("id") Long id, @PathVariable("friendId") Long friendId) throws ConditionsNotMetException, NotFoundException, DuplicatedDataException {
        return userInterface.delFriend(id, friendId);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public Set<User> findJointFriends(@Valid @RequestBody @PathVariable("id") Long id, @PathVariable("otherId") Long otherId) throws ConditionsNotMetException, NotFoundException, DuplicatedDataException {
        return userInterface.findJointFriends(id, otherId);
    }

    @GetMapping("/{id}/friends")
    public Set<User> findJointFriends(@Valid @RequestBody @PathVariable("id") Long id) throws ConditionsNotMetException, NotFoundException, DuplicatedDataException {
        return userInterface.findAllFriends(id);
    }
}