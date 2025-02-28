package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Qualifier("UserDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    // SQL-запросы
    private static final String FIND_ALL_USERS = "SELECT * FROM users";
    private static final String FIND_USER_BY_ID = "SELECT * FROM users WHERE id = ?";
    private static final String INSERT_USER = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_USER = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
    private static final String ADD_FRIEND = "INSERT INTO friends (userId, friendId) VALUES (?, ?)";
    private static final String REMOVE_FRIEND = "DELETE FROM friends WHERE userId = ? AND friendId = ?";
    private static final String GET_FRIENDS = "SELECT u.* FROM users u JOIN friends f ON u.id = f.friendId WHERE f.userId = ?";
    private static final String CHECK_DUPLICATE_EMAIL = "SELECT COUNT(*) FROM users WHERE email = ?";

    // Метод для отображения строки из результата запроса в объект User
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .email(rs.getString("email"))
            .login(rs.getString("login"))
            .name(rs.getString("name"))
            .birthday(rs.getDate("birthday").toLocalDate())
            .friends(new HashSet<>())
            .build();

    @Override
    public Collection<User> findAll() {
        log.info("Returning the list of users from the database...");
        return jdbcTemplate.query(FIND_ALL_USERS, USER_ROW_MAPPER);
    }

    @Override
    public User create(User user) throws ValidationException, DuplicatedDataException {
        validateEmail(user.getEmail());
        validateLogin(user.getLogin());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        validateBirthday(user.getBirthday());

        // Проверка на дублирование email
        if (jdbcTemplate.queryForObject(CHECK_DUPLICATE_EMAIL, Integer.class, user.getEmail()) > 0) {
            throw new DuplicatedDataException("A user with this email already exists");
        }

        // Вставка нового пользователя в базу данных
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("id");
        Map<String, Object> parameters = Map.of(
                "email", user.getEmail(),
                "login", user.getLogin(),
                "name", user.getName(),
                "birthday", user.getBirthday()
        );
        long userId = insert.executeAndReturnKey(parameters).longValue();
        user.setId(userId);

        log.info("User created: {}", user);
        return findById(userId);
    }

    @Override
    public User update(User newUser) throws NotFoundException, ValidationException {
        if (newUser.getId() == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (!existsById(newUser.getId())) {
            throw new NotFoundException("User with ID = " + newUser.getId() + " not found", "Список фильмов с рейтингом пуст.");
        }

        validateEmail(newUser.getEmail());
        validateLogin(newUser.getLogin());
        validateBirthday(newUser.getBirthday());

        // Обновление пользователя в базе данных
        int rowsAffected = jdbcTemplate.update(UPDATE_USER,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName(),
                newUser.getBirthday(),
                newUser.getId());

        if (rowsAffected == 0) {
            throw new NotFoundException("User with ID = " + newUser.getId() + " not found", "Список фильмов с рейтингом пуст.");
        }

        log.info("User with ID = {} updated: {}", newUser.getId(), newUser);
        return findById(newUser.getId());
    }

    @Override
    public User findById(Long id) throws NotFoundException {
        if (id == null) {
            throw new ValidationException("ID cannot be null");
        }
        try {
            return jdbcTemplate.queryForObject(FIND_USER_BY_ID, USER_ROW_MAPPER, id);
        } catch (DataAccessException e) {
            throw new NotFoundException("User with ID = " + id + " not found", "Список фильмов с рейтингом пуст.");
        }
    }

    @Override
    public void addFriend(Long userId, Long friendId) throws NotFoundException {
        if (!existsById(userId)) {
            throw new NotFoundException("User with ID = " + userId + " not found", "Список фильмов с рейтингом пуст.");
        }
        if (!existsById(friendId)) {
            throw new NotFoundException("User with ID = " + friendId + " not found", "Список фильмов с рейтингом пуст.");
        }

        // Добавление друга
        jdbcTemplate.update(ADD_FRIEND, userId, friendId);
        log.info("User with ID = {} added as a friend to user with ID = {}", friendId, userId);
    }

    @Override
    public User removeFriend(Long userId, Long friendId) throws NotFoundException {
        if (!existsById(userId)) {
            throw new NotFoundException("User with ID = " + userId + " not found", "Список фильмов с рейтингом пуст.");
        }
        if (!existsById(friendId)) {
            throw new NotFoundException("User with ID = " + friendId + " not found", "Список фильмов с рейтингом пуст.");
        }

        // Удаление друга
        jdbcTemplate.update(REMOVE_FRIEND, userId, friendId);
        log.info("User with ID = {} has been removed from friends of user with ID = {}", friendId, userId);
        return findById(userId);
    }

    @Override
    public Collection<User> getFriends(Long id) throws NotFoundException {
        if (!existsById(id)) {
            throw new NotFoundException("User with ID = " + id + " not found", "Список фильмов с рейтингом пуст.");
        }

        return jdbcTemplate.query(GET_FRIENDS, USER_ROW_MAPPER, id);
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherUserId) throws NotFoundException {
        if (!existsById(userId)) {
            throw new NotFoundException("User with ID = " + userId + " not found", "Список фильмов с рейтингом пуст.");
        }
        if (!existsById(otherUserId)) {
            throw new NotFoundException("User with ID = " + otherUserId + " not found", "Список фильмов с рейтингом пуст.");
        }

        // Получение общих друзей
        Set<Long> userFriends = getFriendIds(userId);
        Set<Long> otherUserFriends = getFriendIds(otherUserId);

        userFriends.retainAll(otherUserFriends);

        return userFriends.stream()
                .map(this::findById)
                .collect(Collectors.toList());
    }

    private boolean existsById(Long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id) > 0;
    }

    private Set<Long> getFriendIds(Long userId) {
        return jdbcTemplate.queryForList("SELECT friendId FROM friends WHERE userId = ?", Long.class, userId)
                .stream()
                .collect(Collectors.toSet());
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

    public static class FriendsExtractor implements ResultSetExtractor<Map<Long, Set<Long>>> {
        @Override
        public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException {
            Map<Long, Set<Long>> friends = new HashMap<>();
            while (rs.next()) {
                Long userId = rs.getLong("userId");
                Long friendId = rs.getLong("friendId");

                friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
            }
            return friends;
        }
    }
}