package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
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
        log.info("Returning the list of users...");
        return users.values();
    }

    @Override
    public User create(User user) throws ValidationException, DuplicatedDataException {
        validateEmail(user.getEmail());
        validateLogin(user.getLogin());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        validateBirthday(user.getBirthday());
        duplicateCheck(user);
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User newUser) throws NotFoundException, ValidationException {
        if (newUser.getId() == null) {
            log.error("User ID cannot be null");
            throw new ValidationException("User ID cannot be null");
        }
        if (!users.containsKey(newUser.getId())) {
            log.error("User with ID = {} not found", newUser.getId());
            throw new NotFoundException("User with ID = " + newUser.getId() + " not found");
        }
        User oldUser = users.get(newUser.getId());
        oldUser.setEmail(newUser.getEmail());
        oldUser.setLogin(newUser.getLogin());
        oldUser.setName(newUser.getName() != null ? newUser.getName() : newUser.getLogin());
        oldUser.setBirthday(newUser.getBirthday());
        return oldUser;
    }

    @Override
    public User findById(Long id) throws NotFoundException {
        if (id == null) {
            log.error("ID cannot be null");
            throw new ValidationException("ID cannot be null");
        }
        User user = users.get(id);
        if (user == null) {
            log.error("User with ID = {} not found", id);
            throw new NotFoundException("User with ID = " + id + " not found");
        }
        return user;
    }

    @Override
    public void addFriend(Long userId, Long friendId) throws NotFoundException {
        User user = findById(userId);
        User friend = findById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("User with ID = {} has been added as a friend to user with ID = {}", friendId, userId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) throws NotFoundException {
        User user = findById(userId);
        User friend = findById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        log.info("User with ID = {} has been removed from friends of user with ID = {}", friendId, userId);
    }

    @Override
    public Collection<User> getFriends(Long id) throws NotFoundException {
        User user = findById(id);
        // Возвращаем пустой список, если у пользователя нет друзей
        return user.getFriends().isEmpty() ? Collections.emptyList() : user.getFriends().stream()
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

    private void duplicateCheck(User user) throws DuplicatedDataException {
        for (User u : users.values()) {
            if (u.getEmail().equals(user.getEmail())) {
                log.error("A user with this email already exists: {}", user.getEmail());
                throw new DuplicatedDataException("A user with this email already exists");
            }
        }
    }

    private void validateEmail(String email) throws ValidationException {
        if (email == null || email.isBlank() || !email.contains("@") || email.contains(" ") || email.length() < 2) {
            log.error("Invalid email: {}", email);
            throw new ValidationException("Invalid email");
        }
    }

    private void validateLogin(String login) throws ValidationException {
        if (login == null || login.isBlank() || login.contains(" ")) {
            log.error("Invalid login: {}", login);
            throw new ValidationException("Login cannot be empty or contain spaces");
        }
    }

    private void validateBirthday(LocalDate birthday) throws ValidationException {
        if (birthday == null) {
            log.error("Birthday cannot be null");
            throw new ValidationException("Birthday cannot be null");
        }
        if (birthday.isAfter(LocalDate.now())) {
            log.error("Birthday cannot be in the future");
            throw new ValidationException("Birthday cannot be in the future");
        }
    }
}