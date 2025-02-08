package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.controller.UserController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerTest {

    private static UserController userController;
    private static User user;
    private static User user10;
    private static User user1;
    private static User user2;
    private static User user3;
    private static User user4;
    private static User user5;
    private static User user6;

    @BeforeAll
    public static void start() throws ValidationException, DuplicatedDataException {
        userController = new UserController(new UserService());

        user10 = User.of(0L, "name111", "name1113@mail.ru", "name111@mail", LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        userController.create(user10);

        user = User.of(0L, "name111", "name111@mail.ru", "name111@mail", LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        user1 = User.of(0L, "name1", "name111mail.ru", "name111@mail", LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // Invalid email

        user2 = User.of(0L, "name1", "name111@mail.ru", "name111@mail", LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // Duplicate email

        user3 = User.of(0L, "name1", "name161@mail.ru", " ", LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // Invalid login

        user4 = User.of(0L, "name1", "name181@mail.ru", "name111@mail", LocalDate.parse("2028-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // Invalid birthday

        user5 = User.of(null, "name1", "name111@mail.ru", "name119@mail", LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // No ID

        user6 = User.of(600L, "name1", "name911@mail.ru", "name111@mail", LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // Assuming this ID does not exist
    }

    @Test
    public void shouldCreateUser() throws ValidationException, DuplicatedDataException {
        assertEquals(userController.create(user), user);
    }

    @Test
    public void shouldThrowValidationExceptionForInvalidEmail() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.create(user1);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowDuplicatedDataExceptionForDuplicateEmail() {
        DuplicatedDataException exception = assertThrows(DuplicatedDataException.class, () -> {
            userController.create(user2);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowValidationExceptionForInvalidLogin() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.create(user3);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowValidationExceptionForInvalidBirthday() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.create(user4);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowValidationExceptionForUserWithNoId() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.update(user5);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowNotFoundExceptionForWrongId() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            userController.update(user6);
        });
        assertNotNull(exception);
    }

    @AfterAll
    public static void shouldReturnAllUsers() {
        assertNotNull(userController.findAll());
    }
}