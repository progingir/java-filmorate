package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryUserStorageTest {

    private InMemoryUserStorage userStorage;

    @BeforeEach
    public void setUp() {
        userStorage = new InMemoryUserStorage();
    }

    @Test
    public void shouldAddFriendAndUpdateBothUsers() throws Exception {
        User user1 = User.of(null, "User  1", "user1@example.com", "user1", LocalDate.of(1990, 1, 1), new HashSet<>());
        User user2 = User.of(null, "User  2", "user2@example.com", "user2", LocalDate.of(1995, 5, 5), new HashSet<>());

        userStorage.create(user1);
        userStorage.create(user2);

        userStorage.addFriend(user1.getId(), user2.getId());

        List<User> friendsOfUser1 = (List<User>) userStorage.getFriends(user1.getId());
        assertEquals(1, friendsOfUser1.size());
        assertEquals(user2.getId(), friendsOfUser1.get(0).getId());

        List<User> friendsOfUser2 = (List<User>) userStorage.getFriends(user2.getId());
        assertEquals(1, friendsOfUser2.size());
        assertEquals(user1.getId(), friendsOfUser2.get(0).getId());
    }

    @Test
    public void shouldRemoveFriendAndUpdateBothUsers() throws Exception {
        User user1 = User.of(null, "User 1", "user1@example.com", "user1", LocalDate.of(1990, 1, 1), null);
        User user2 = User.of(null, "User 2", "user2@example.com", "user2", LocalDate.of(1995, 5, 5), null);

        userStorage.create(user1);
        userStorage.create(user2);

        userStorage.addFriend(user1.getId(), user2.getId());

        userStorage.removeFriend(user1.getId(), user2.getId());

        assertTrue(((List<User>) userStorage.getFriends(user1.getId())).isEmpty());
        assertTrue(((List<User>) userStorage.getFriends(user2.getId())).isEmpty());
    }

    @Test
    public void shouldGetCommonFriends() throws Exception {
        User user1 = User.of(null, "User  1", "user1@example.com", "user1", LocalDate.of(1990, 1, 1), new HashSet<>());
        User user2 = User.of(null, "User  2", "user2@example.com", "user2", LocalDate.of(1995, 5, 5), new HashSet<>());
        User user3 = User.of(null, "User  3", "user3@example.com", "user3", LocalDate.of(2000, 1, 1), new HashSet<>());

        userStorage.create(user1);
        userStorage.create(user2);
        userStorage.create(user3);

        userStorage.addFriend(user1.getId(), user3.getId());
        userStorage.addFriend(user2.getId(), user3.getId());

        List<User> commonFriends = (List<User>) userStorage.getCommonFriends(user1.getId(), user2.getId());

        assertEquals(1, commonFriends.size());
        assertEquals(user3.getId(), commonFriends.get(0).getId());
    }

    @Test
    public void shouldThrowNotFoundExceptionForUnknownUser() {
        User user1 = User.of(null, "User  1", "user1@example.com", "user1", LocalDate.of(1990, 1, 1), new HashSet<>());
        userStorage.create(user1);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            userStorage.addFriend(user1.getId(), 999L);
        });
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("not found"));

        exception = assertThrows(NotFoundException.class, () -> {
            userStorage.removeFriend(user1.getId(), 999L);
        });
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("not found"));
    }
}