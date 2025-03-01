package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final JdbcTemplate jdbcTemplate;
    private final UserStorage userStorage;

    // SQL-запросы
    private final String sqlQuery1 = "select id, name, description, releaseDate, duration from film";
    private final String sqlQuery2 = "select filmId, userId from likedUsers";
    private final String sqlQuery3 = "select filmId, genreId from filmGenre";
    private final String sqlQuery4 = "select id, ratingId from film";
    private final String sqlQuery5 = "select id, name, description, releaseDate, duration from film where id = ?";
    private final String sqlQuery6 = "select filmId, userId from likedUsers where filmId = ?";
    private final String sqlQuery7 = "select filmId, genreId from filmGenre where filmId = ?";
    private final String sqlQuery8 = "select id, ratingId from film where id = ?";
    private final String sqlQuery9 = "select id, name from genre";
    private final String sqlQuery10 = "select id, rating from filmrating";
    private final String sqlQuery11 = "delete from filmGenre where filmId = ?";
    private final String sqlQuery12 = "insert into filmGenre(filmId, genreId) values (?, ?)";
    private final String sqlQuery13 = "update film set name = ?, description = ?, releaseDate = ?, duration = ?, ratingId = ? where id = ?";
    private final String sqlQuery14 = "update film set ratingId = ? where id = ?";

    // Маппер для преобразования строки ResultSet в объект Film
    private static final RowMapper<Film> FILM_ROW_MAPPER = (rs, rowNum) -> Film.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .releaseDate(rs.getDate("releaseDate").toLocalDate())
            .duration(rs.getInt("duration"))
            .likedUsers(new HashSet<>()) // Лайки будут загружаться отдельно
            .mpa(Mpa.of(rs.getLong("ratingId"), null)) // Используем статический метод of
            .genres(new LinkedHashSet<>()) // Жанры будут загружены отдельно
            .build();

    @Override
    public Collection<Film> findAll() {
        log.info("Processing Get-request...");
        List<Film> films = jdbcTemplate.query(sqlQuery1, FILM_ROW_MAPPER);
        films.forEach(this::loadAdditionalData); // Загружаем дополнительные данные (лайки, жанры, MPA)
        return films;
    }

    @Override
    public Film findById(Long id) throws NotFoundException {
        if (id == null) {
            throw new ValidationException(ExceptionMessages.FILM_ID_CANNOT_BE_NULL);
        }
        try {
            Film film = jdbcTemplate.queryForObject(sqlQuery5, FILM_ROW_MAPPER, id);
            if (film != null) {
                loadAdditionalData(film); // Загружаем дополнительные данные (лайки, жанры, MPA)
            }
            return film;
        } catch (DataAccessException e) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, id), "Список фильмов с рейтингом пуст.");
        }
    }

    @Override
    public Film create(Film film) throws ValidationException {
        validateFilm(film);

        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("film")
                .usingGeneratedKeyColumns("id");
        Map<String, Object> parameters = Map.of(
                "name", film.getName(),
                "description", film.getDescription(),
                "releaseDate", film.getReleaseDate(),
                "duration", film.getDuration(),
                "ratingId", film.getMpa().getId()
        );
        long filmId = insert.executeAndReturnKey(parameters).longValue();
        film.setId(filmId);

        // Сохраняем жанры
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            jdbcTemplate.update(sqlQuery11, filmId); // Удаляем старые жанры
            for (Genre genre : film.getGenres()) {
                jdbcTemplate.update(sqlQuery12, filmId, genre.getId()); // Вставляем новые жанры
            }
        }

        log.info("Film created: {}", film);
        return findById(filmId);
    }

    @Override
    public Film update(Film film) throws NotFoundException, ValidationException {
        validateFilm(film);

        if (!existsById(film.getId())) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, film.getId()), "Список фильмов с рейтингом пуст.");
        }

        jdbcTemplate.update(sqlQuery13,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        // Обновляем жанры
        if (film.getGenres() != null) {
            jdbcTemplate.update(sqlQuery11, film.getId()); // Удаляем старые жанры
            for (Genre genre : film.getGenres()) {
                jdbcTemplate.update(sqlQuery12, film.getId(), genre.getId()); // Вставляем новые жанры
            }
        }

        log.info("Film with ID = {} updated: {}", film.getId(), film);
        return findById(film.getId());
    }

    public void addLike(Long filmId, Long userId) throws NotFoundException {
        if (!existsById(filmId)) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, filmId), "Список фильмов с рейтингом пуст.");
        }
        if (userStorage.findById(userId) == null) {
            throw new NotFoundException(String.format("User with ID = %d not found", userId), "Список фильмов с рейтингом пуст.");
        }

        jdbcTemplate.update("INSERT INTO likedUsers (filmId, userId) VALUES (?, ?)", filmId, userId);
        log.info("User with ID = {} liked the film with ID = {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) throws NotFoundException {
        if (!existsById(filmId)) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, filmId), "Список фильмов с рейтингом пуст.");
        }
        if (userStorage.findById(userId) == null) {
            throw new NotFoundException(String.format("User with ID = %d not found", userId), "Список фильмов с рейтингом пуст.");
        }

        jdbcTemplate.update("DELETE FROM likedUsers WHERE filmId = ? AND userId = ?", filmId, userId);
        log.info("User with ID = {} unliked the film with ID = {}", userId, filmId);
    }

    public List<Film> getTopFilms(int count) {
        log.info("Getting top-{} films by number of likes", count);
        String sql = "SELECT f.* FROM film f LEFT JOIN likedUsers l ON f.id = l.filmId GROUP BY f.id ORDER BY COUNT(l.userId) DESC LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, FILM_ROW_MAPPER, count);
        films.forEach(this::loadAdditionalData); // Загружаем дополнительные данные (лайки, жанры, MPA)
        return films;
    }

    private boolean existsById(Long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM film WHERE id = ?", Integer.class, id) > 0;
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

    // Метод для загрузки дополнительных данных (лайки, жанры, MPA)
    private void loadAdditionalData(Film film) {
        Long filmId = film.getId();

        // Загружаем лайки
        Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(sqlQuery6, new LikedUsersExtractor(), filmId);
        film.setLikedUsers(likedUsers.getOrDefault(filmId, new HashSet<>()));

        // Загружаем жанры
        Map<Long, LinkedHashSet<Long>> filmGenres = jdbcTemplate.query(sqlQuery7, new FilmGenreExtractor(), filmId);
        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        if (filmGenres.containsKey(filmId)) {
            for (Long genreId : filmGenres.get(filmId)) {
                // Используем статический метод of для создания объекта Genre
                genres.add(Genre.of(genreId, null)); // Название жанра будет загружено отдельно
            }
        }
        film.setGenres(genres);

        // Загружаем MPA
        Long ratingId = jdbcTemplate.queryForObject(sqlQuery8, Long.class, filmId);
        String ratingName = jdbcTemplate.queryForObject("SELECT rating FROM filmrating WHERE id = ?", String.class, ratingId);
        film.setMpa(Mpa.of(ratingId, ratingName)); // Используем статический метод of
    }

    // Extractor для лайков
    public static class LikedUsersExtractor implements ResultSetExtractor<Map<Long, Set<Long>>> {
        @Override
        public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException {
            Map<Long, Set<Long>> likedUsers = new HashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("filmId");
                Long userId = rs.getLong("userId");
                likedUsers.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
            }
            return likedUsers;
        }
    }

    // Extractor для жанров
    public static class FilmGenreExtractor implements ResultSetExtractor<Map<Long, LinkedHashSet<Long>>> {
        @Override
        public Map<Long, LinkedHashSet<Long>> extractData(ResultSet rs) throws SQLException {
            Map<Long, LinkedHashSet<Long>> filmGenres = new HashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("filmId");
                Long genreId = rs.getLong("genreId");
                filmGenres.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genreId);
            }
            return filmGenres;
        }
    }
}