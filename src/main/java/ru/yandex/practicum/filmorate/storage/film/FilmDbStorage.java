package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final UserStorage userStorage;

    // SQL-запросы
    private static final String FIND_ALL_FILMS = "SELECT * FROM films";
    private static final String FIND_FILM_BY_ID = "SELECT * FROM films WHERE id = ?";
    private static final String INSERT_FILM = "INSERT INTO films (name, description, release_date, duration) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_FILM = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ? WHERE id = ?";
    private static final String ADD_LIKE = "INSERT INTO likes (filmID, userID) VALUES (?, ?)";
    private static final String REMOVE_LIKE = "DELETE FROM likes WHERE filmID = ? AND userID = ?";
    private static final String GET_TOP_FILMS = "SELECT f.* FROM films f LEFT JOIN likes l ON f.id = l.filmID GROUP BY f.id ORDER BY COUNT(l.userID) DESC LIMIT ?";

    // Метод для отображения строки из результата запроса в объект Film
    private static final RowMapper<Film> FILM_ROW_MAPPER = (rs, rowNum) -> Film.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .releaseDate(rs.getDate("release_date").toLocalDate())
            .duration(rs.getInt("duration"))
            .likedUsers(new HashSet<>()) // Лайки будут загружаться отдельно
            .build();

    @Override
    public Collection<Film> findAll() {
        log.info("Processing Get-request...");
        return jdbcTemplate.query(FIND_ALL_FILMS, FILM_ROW_MAPPER);
    }

    @Override
    public Film findById(Long id) throws NotFoundException {
        if (id == null) {
            throw new ValidationException(ExceptionMessages.FILM_ID_CANNOT_BE_NULL);
        }
        try {
            return jdbcTemplate.queryForObject(FIND_FILM_BY_ID, FILM_ROW_MAPPER, id);
        } catch (DataAccessException e) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, id));
        }
    }

    @Override
    public Film create(Film film) throws ValidationException {
        validateFilm(film);

        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("id");
        Map<String, Object> parameters = Map.of(
                "name", film.getName(),
                "description", film.getDescription(),
                "release_date", film.getReleaseDate(),
                "duration", film.getDuration()
        );
        long filmId = insert.executeAndReturnKey(parameters).longValue();
        film.setId(filmId);

        log.info("Film created: {}", film);
        return findById(filmId);
    }

    @Override
    public Film update(Film film) throws NotFoundException, ValidationException {
        validateFilm(film);

        if (!existsById(film.getId())) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, film.getId()));
        }

        int rowsAffected = jdbcTemplate.update(UPDATE_FILM,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getId());

        if (rowsAffected == 0) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, film.getId()));
        }

        log.info("Film with ID = {} updated: {}", film.getId(), film);
        return findById(film.getId());
    }

    @Override
    public void addLike(Long filmId, Long userId) throws NotFoundException {
        if (!existsById(filmId)) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, filmId));
        }
        if (userStorage.findById(userId) == null) {
            throw new NotFoundException(String.format("User with ID = %d not found", userId));
        }

        jdbcTemplate.update(ADD_LIKE, filmId, userId);
        log.info("User with ID = {} liked the film with ID = {}", userId, filmId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) throws NotFoundException {
        if (!existsById(filmId)) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, filmId));
        }
        if (userStorage.findById(userId) == null) {
            throw new NotFoundException(String.format("User with ID = %d not found", userId));
        }

        int rowsAffected = jdbcTemplate.update(REMOVE_LIKE, filmId, userId);
        if (rowsAffected == 0) {
            throw new NotFoundException(String.format("User with ID = %d did not like the film with ID = %d", userId, filmId));
        }

        log.info("User with ID = {} unliked the film with ID = {}", userId, filmId);
    }

    @Override
    public List<Film> getTopFilms(int count) {
        log.info("Getting top-{} films by number of likes", count);
        return jdbcTemplate.query(GET_TOP_FILMS, FILM_ROW_MAPPER, count);
    }

    private boolean existsById(Long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM films WHERE id = ?", Integer.class, id) > 0;
    }

    private void validateFilm(Film film) throws ValidationException {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException(ExceptionMessages.FILM_DURATION_INVALID);
        }
    }
}