package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Collection<User> findAll() {
        return userService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody User user) throws ValidationException, DuplicatedDataException {
        return userService.create(user);
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) throws NotFoundException, ValidationException {
        return userService.update(user);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) throws NotFoundException, ValidationException {
        return userService.findById(id);
    }

    @PutMapping("/{id}/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) throws NotFoundException {
        userService.addFriend(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@PathVariable Long id, @PathVariable Long friendId) throws NotFoundException {
        userService.removeFriend(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public Collection<User> getFriends(@PathVariable Long id) throws NotFoundException {
        return userService.getFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public Collection<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) throws NotFoundException {
        return userService.getCommonFriends(id, otherId);
    }
}