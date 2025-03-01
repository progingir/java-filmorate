package ru.yandex.practicum.filmorate.storage.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "TRACE")
@ConfigurationPropertiesScan
@Component
@Qualifier("UserDbStorage")
public class UserDbStorage implements UserStorage {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final JdbcTemplate jdbcTemplate;

    private final String sqlQuery1 = "select id, name, email, login, birthday from users";
    private final String sqlQuery2 = "select userId, friendId from friends";
    private final String sqlQuery3 = "select id, name, email, login, birthday from users where id = ?";
    private final String sqlQuery4 = "select email from users";
    private final String sqlQuery5 = "update users set " + "name = ?, email = ?, login = ?, birthday = ? " + "where id = ?";

    private User mapRowToUser(ResultSet resultSet, int rowNum) throws SQLException {
        return User.builder().id(resultSet.getLong("id")).name(resultSet.getString("name")).email(resultSet.getString("email")).login(resultSet.getString("login")).birthday(resultSet.getDate("birthday").toLocalDate()).friends(new HashSet<>()).friendRequests(new HashSet<>()).build();
    }

    public static class FriendsExtractor implements ResultSetExtractor<Map<Long, Set<Long>>> {
        @Override
        public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException {
            Map<Long, Set<Long>> data = new LinkedHashMap<>();
            while (rs.next()) {
                Long userId = rs.getLong("userId");
                data.putIfAbsent(userId, new HashSet<>());
                Long friendId = rs.getLong("friendId");
                data.get(userId).add(friendId);
            }
            return data;
        }
    }

    public static class EmailExtractor implements ResultSetExtractor<Set<String>> {
        @Override
        public Set<String> extractData(ResultSet rs) throws SQLException {
            Set<String> data = new HashSet<>();
            while (rs.next()) {
                String email = rs.getString("email");
                data.add(email);
            }
            return data;
        }
    }

    public Collection<User> findAll() {
        log.info("Обработка Get-запроса...");
        Collection<User> users = jdbcTemplate.query(sqlQuery1, this::mapRowToUser);
        Map<Long, Set<Long>> friends = jdbcTemplate.query(sqlQuery2, new FriendsExtractor());
        for (User user : users) {
            user.setFriends(friends.get(user.getId()));
        }
        return users;
    }

    public User findById(Long id) throws ConditionsNotMetException {
        log.info("Обработка Get-запроса...");
        if (id != 0 && !id.equals(null)) {
            try {
                jdbcTemplate.queryForObject(sqlQuery3, this::mapRowToUser, id);
            } catch (DataAccessException e) {
                if (e != null) {
                    log.error("Exception", new NotFoundException(id.toString(), "Пользователь с данным идентификатором отсутствует в базе"));
                    throw new NotFoundException(id.toString(), "Пользователь с данным идентификатором отсутствует в базе");
                }
            }
            User user = jdbcTemplate.queryForObject(sqlQuery3, this::mapRowToUser, id);
            Map<Long, Set<Long>> friends = jdbcTemplate.query(sqlQuery2, new FriendsExtractor());
            user.setFriends(friends.get(id));
            return user;
        } else {
            log.error("Exception", new ConditionsNotMetException("Идентификатор пользователя не может быть нулевой"));
            throw new ConditionsNotMetException("Идентификатор пользователя не может быть нулевой");
        }
    }

    public Collection<User> getFriends(Long id) throws NotFoundException {
        return List.of();
    }

    public User create(@Valid User user) throws DuplicatedDataException, ValidationException {
        log.info("Обработка Create-запроса...");

        // Проверка на дубликат email
        duplicateCheck(user);

        // Проверка email
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }

        // Проверка логина
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }

        // Проверка имени
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        // Проверка даты рождения
        if (user.getBirthday() == null) {
            throw new ValidationException("Дата рождения не может быть нулевой");
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        // Создание пользователя
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("id");
        Long id = simpleJdbcInsert.executeAndReturnKey(user.toMapUser()).longValue();
        user.setId(id);

        return user;
    }

    private void duplicateCheck(User user) throws DuplicatedDataException {
        Set<String> emails = jdbcTemplate.query(sqlQuery4, new EmailExtractor());
        if (emails.contains(user.getEmail())) {
            log.error("Exception", new DuplicatedDataException("Этот имейл уже используется"));
            throw new DuplicatedDataException("Этот имейл уже используется");
        }
    }

    public User update(@Valid User newUser) throws NotFoundException, DuplicatedDataException, ValidationException {
        log.info("Обработка Update-запроса...");

        // Проверка на наличие ID
        if (newUser.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }

        // Поиск существующего пользователя
        User oldUser = findById(newUser.getId());
        if (oldUser == null) {
            throw new NotFoundException("oldUser", "Пользователь с указанным id не найден");
        }

        // Проверка email
        if (newUser.getEmail() == null || newUser.getEmail().isBlank() || !newUser.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }

        // Проверка на дубликат email
        if (!newUser.getEmail().equals(oldUser.getEmail())) {
            duplicateCheck(newUser);
        }

        // Проверка логина
        if (newUser.getLogin() == null || newUser.getLogin().isBlank() || newUser.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }

        // Проверка имени
        if (newUser.getName() == null || newUser.getName().isBlank()) {
            newUser.setName(newUser.getLogin());
        }

        // Проверка даты рождения
        if (newUser.getBirthday() == null) {
            throw new ValidationException("Дата рождения не может быть нулевой");
        }
        if (newUser.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        // Обновление пользователя
        jdbcTemplate.update(sqlQuery5,
                newUser.getName(),
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getBirthday(),
                newUser.getId());

        return newUser;
    }
}