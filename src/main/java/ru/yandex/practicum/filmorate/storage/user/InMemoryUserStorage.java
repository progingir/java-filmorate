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
        user.setFriends(new HashSet<>()); // Ensure friends list is initialized
        users.put(user.getId(), user);
        log.info("User created: {}", user);
        return user;
    }

    @Override
    public User update(User newUser) throws NotFoundException, ValidationException {
        if (newUser.getId() == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (!users.containsKey(newUser.getId())) {
            throw new NotFoundException("User with ID = " + newUser.getId() + " not found");
        }
        User oldUser = users.get(newUser.getId());
        oldUser.setEmail(newUser.getEmail());
        oldUser.setLogin(newUser.getLogin());
        oldUser.setName(newUser.getName() != null ? newUser.getName() : newUser.getLogin());
        oldUser.setBirthday(newUser.getBirthday());

        // Initialize friends list if null
        if (newUser.getFriends() == null) {
            newUser.setFriends(new HashSet<>());
        }
        oldUser.setFriends(new HashSet<>(newUser.getFriends()));

        users.put(oldUser.getId(), oldUser);
        log.info("User with ID = {} updated: {}", oldUser.getId(), oldUser);
        return oldUser;
    }

    @Override
    public User findById(Long id) throws NotFoundException {
        if (id == null) {
            throw new ValidationException("ID cannot be null");
        }
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("User with ID = " + id + " not found");
        }
        log.info("User found: {}", user);
        return user;
    }

    @Override
    public void addFriend(Long userId, Long friendId) throws NotFoundException {
        User user = findById(userId);
        User friend = findById(friendId);

        // Ensure friends lists are initialized
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        if (friend.getFriends() == null) {
            friend.setFriends(new HashSet<>());
        }

        // Add friends in both directions
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        // Save changes
        update(user);
        update(friend);

        log.info("User with ID = {} added as a friend to user with ID = {}", friendId, userId);
    }

    @Override
    public User removeFriend(Long userId, Long friendId) throws NotFoundException {
        User user = findById(userId);
        User friend = findById(friendId);

        // Ensure friends lists are initialized
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        if (friend.getFriends() == null) {
            friend.setFriends(new HashSet<>());
        }

        // Remove friends in both directions
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.info("User with ID = {} has been removed from friends of user with ID = {}", friendId, userId);
        return user;
    }

    @Override
    public Collection<User> getFriends(Long id) throws NotFoundException {
        User user = findById(id);

        // Return empty list if no friends exist
        if (user.getFriends() == null || user.getFriends().isEmpty()) {
            return Collections.emptyList();
        }

        return user.getFriends().stream()
                .map(this::findById)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherUserId) throws NotFoundException {
        User user = findById(userId);
        User otherUser = findById(otherUserId);

        // Ensure friends lists are initialized
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        if (otherUser.getFriends() == null) {
            otherUser.setFriends(new HashSet<>());
        }

        Set<Long> commonFriendIds = new HashSet<>(user.getFriends());
        commonFriendIds.retainAll(otherUser.getFriends());

        return commonFriendIds.stream()
                .map(this::findById)
                .collect(Collectors.toList());
    }

    private long getNextId() {
        return users.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    private void duplicateCheck(User user) throws DuplicatedDataException {
        for (User u : users.values()) {
            if (u.getEmail().equals(user.getEmail())) {
                throw new DuplicatedDataException("A user with this email already exists");
            }
        }
    }

    private void validateEmail(String email) throws ValidationException {
        if (email == null || email.isBlank() || !email.contains("@") || email.contains(" ") || email.length() < 2) {
            throw new ValidationException("Invalid email");
        }
    }

    private void validateLogin(String login) throws ValidationException {
        if (login == null || login.isBlank() || login.contains(" ")) {
            throw new ValidationException("Login cannot be empty or contain spaces");
        }
    }

    private void validateBirthday(LocalDate birthday) throws ValidationException {
        if (birthday == null) {
            throw new ValidationException("Birthday cannot be null");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new ValidationException("Birthday cannot be in the future");
        }
    }
}