package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j(topic = "TRACE")
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class UserService implements UserInterface {

    //sQL-запросы
    private static final String SQL_SELECT_FRIENDS = "select userId, friendId from friends";
    private static final String SQL_INSERT_FRIEND = "insert into friends(userId, friendId) values (?, ?)";
    private static final String SQL_DELETE_FRIEND = "delete from friends where userId = ? and friendId = ?";

    //сообщения для логирования и исключений
    private static final String LOG_POST_REQUEST = "Обработка Post-запроса...";
    private static final String LOG_DELETE_REQUEST = "Обработка Del-запроса...";
    private static final String LOG_GET_REQUEST = "Обработка Get-запроса...";
    private static final String ERROR_FRIEND_ALREADY_ADDED = "Пользователь с id %d уже добавлен в друзья";
    private static final String ERROR_USER_NOT_FOUND = "Пользователь с данным идентификатором отсутствует в базе";

    @Autowired
    @Qualifier("UserDbStorage")
    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public User addFriend(Long idUser, Long idFriend) {
        log.info(LOG_POST_REQUEST);

        validateUserExists(idUser);
        validateUserExists(idFriend);

        if (isFriend(idUser, idFriend)) {
            logAndThrowConditionsNotMetException(String.format(ERROR_FRIEND_ALREADY_ADDED, idFriend));
        }

        jdbcTemplate.update(SQL_INSERT_FRIEND, idUser, idFriend);
        return userStorage.findById(idUser);
    }

    @Override
    public User delFriend(Long idUser, Long idFriend) {
        log.info(LOG_DELETE_REQUEST);

        validateUserExists(idUser);
        validateUserExists(idFriend);

        jdbcTemplate.update(SQL_DELETE_FRIEND, idUser, idFriend);
        return userStorage.findById(idUser);
    }

    @Override
    public Set<User> findJointFriends(Long idUser, Long idFriend) {
        log.info(LOG_GET_REQUEST);

        validateUserExists(idUser);
        validateUserExists(idFriend);

        // SQL-запрос для поиска общих друзей
        String sql = "SELECT f1.friend_id " +
                "FROM friends f1 " +
                "INNER JOIN friends f2 ON f1.friend_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";

        // Выполняем запрос и получаем список ID общих друзей
        List<Long> jointFriendIds = jdbcTemplate.queryForList(sql, Long.class, idUser, idFriend);

        // Преобразуем список ID в Set<User>
        Set<User> result = new HashSet<>();
        for (Long friendId : jointFriendIds) {
            result.add(userStorage.findById(friendId));
        }

        return result;
    }

    @Override
    public Set<User> findAllFriends(Long idUser) {
        log.info(LOG_GET_REQUEST);

        validateUserExists(idUser);

        Map<Long, Set<Long>> friends = jdbcTemplate.query(SQL_SELECT_FRIENDS, new UserDbStorage.FriendsExtractor());
        assert friends != null;
        Set<Long> userFriends = friends.getOrDefault(idUser, new HashSet<>());

        Set<User> result = new HashSet<>();
        for (Long friendId : userFriends) {
            result.add(userStorage.findById(friendId));
        }
        return result;
    }

    private void validateUserExists(Long userId) {
        if (userStorage.findById(userId) == null) {
            logAndThrowNotFoundException(userId.toString());
        }
    }

    private boolean isFriend(Long idUser, Long idFriend) {
        Map<Long, Set<Long>> friends = jdbcTemplate.query(SQL_SELECT_FRIENDS, new UserDbStorage.FriendsExtractor());
        assert friends != null;
        return friends.getOrDefault(idUser, new HashSet<>()).contains(idFriend);
    }

    private void logAndThrowConditionsNotMetException(String message) {
        log.error("Exception", new ConditionsNotMetException(message));
        throw new ConditionsNotMetException(message);
    }

    private void logAndThrowNotFoundException(String value) {
        log.error("Exception", new NotFoundException(UserService.ERROR_USER_NOT_FOUND));
        throw new NotFoundException(UserService.ERROR_USER_NOT_FOUND);
    }
}