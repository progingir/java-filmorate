package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;

import static java.nio.file.Paths.get;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest {

    private static UserController userController;
    private static User validUser;
    private static User invalidEmailUser;
    private static User duplicateEmailUser;
    private static User invalidLoginUser;
    private static User userWithNoId;
    private static User userWithWrongId;

    @BeforeAll
    public static void start() throws ValidationException, DuplicatedDataException {
        UserStorage userStorage = new InMemoryUserStorage();
        userController = new UserController(new UserService(userStorage));

        // Валидный пользователь
        validUser = User.of(0L, "Valid User", "valid@mail.ru", "validLogin", LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")), new HashSet<>());
        userController.create(validUser);

        // Пользователь с некорректным email
        invalidEmailUser = User.of(0L, "Invalid Email User", "invalidEmail", "invalidLogin", LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")), new HashSet<>());

        // Пользователь с дублирующимся email
        duplicateEmailUser = User.of(0L, "Duplicate Email User", "valid@mail.ru", "duplicateLogin", LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")), new HashSet<>());

        // Пользователь с некорректным логином (с пробелами)
        invalidLoginUser = User.of(0L, "Invalid Login User", "login@mail.ru", "invalid login", LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")), new HashSet<>());

        // Пользователь без ID
        userWithNoId = User.of(null, "No ID User", "noid@mail.ru", "noIdLogin", LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")), new HashSet<>());

        // Пользователь с несуществующим ID
        userWithWrongId = User.of(999L, "Wrong ID User", "wrongid@mail.ru", "wrongIdLogin", LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")), new HashSet<>());
    }

    @Test
    public void shouldThrowExceptionWhenCreatingUserWithInvalidEmail() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.create(invalidEmailUser);
        });
        assertNotNull(exception);
        assertEquals("Invalid email", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenCreatingUserWithDuplicateEmail() {
        DuplicatedDataException exception = assertThrows(DuplicatedDataException.class, () -> {
            userController.create(duplicateEmailUser);
        });
        assertNotNull(exception);
        assertEquals("A user with this email already exists", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenCreatingUserWithInvalidLogin() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.create(invalidLoginUser);
        });
        assertNotNull(exception);
        assertEquals("Login cannot be empty or contain spaces", exception.getMessage());
    }


    @Test
    public void shouldThrowExceptionWhenUpdatingUserWithNoId() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userController.update(userWithNoId);
        });
        assertNotNull(exception);
        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingUserWithWrongId() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            userController.update(userWithWrongId);
        });
        assertNotNull(exception);
        assertEquals("User with ID = 999 not found", exception.getMessage());
    }
}