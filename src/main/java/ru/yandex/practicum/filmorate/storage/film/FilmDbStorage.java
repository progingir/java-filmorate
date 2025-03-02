package ru.yandex.practicum.filmorate.storage.film;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.service.GenreExtractor;
import ru.yandex.practicum.filmorate.service.RatingNameExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "TRACE")
@ConfigurationPropertiesScan
@Component
@Qualifier("FilmDbStorage")
public class FilmDbStorage implements FilmStorage {

    //SQL-запросы
    private static final String SQL_SELECT_GENRES = "select id, name from genre";
    private static final String SQL_SELECT_RATINGS = "select id, rating from filmrating";
    private static final String SQL_INSERT_FILM_GENRE = "insert into filmGenre(filmId, genreId) values (?, ?)";
    private static final String SQL_UPDATE_FILM_RATING = "update film set ratingId = ? where id = ?";
    private static final String SQL_UPDATE_FILM = "update film set name = ?, description = ?, releaseDate = ?, duration = ?, ratingId = ? where id = ?";

    //сообщения для логирования и исключений
    private static final String LOG_GET_REQUEST = "Обработка Get-запроса...";
    private static final String LOG_CREATE_REQUEST = "Обработка Create-запроса...";
    private static final String LOG_UPDATE_REQUEST = "Обработка Put-запроса...";
    private static final String ERROR_NULL_ID = "Идентификатор фильма не может быть нулевой";
    private static final String ERROR_FILM_NOT_FOUND = "Идентификатор фильма отсутствует в базе";
    private static final String ERROR_EMPTY_NAME = "Название не может быть пустым";
    private static final String ERROR_DESCRIPTION_LENGTH = "Максимальная длина описания — 200 символов";
    private static final String ERROR_RELEASE_DATE = "Дата релиза — не раньше 28 декабря 1895 года";
    private static final String ERROR_DURATION = "Продолжительность фильма должна быть положительным числом";
    private static final String ERROR_INVALID_RATING = "Некорректный рейтинг";
    private static final String ERROR_INVALID_GENRE = "Некорректный жанр";

    private final JdbcTemplate jdbcTemplate;

    private Film mapRowToFilm(ResultSet resultSet, int rowNum) throws SQLException {
        return Film.builder()
                .id(resultSet.getLong("id"))
                .name(resultSet.getString("name"))
                .description(resultSet.getString("description"))
                .releaseDate(resultSet.getDate("releaseDate").toLocalDate())
                .duration(resultSet.getInt("duration"))
                .build();
    }

    public static class LikedUsersExtractor implements ResultSetExtractor<Map<Long, Set<Long>>> {
        @Override
        public Map<Long, Set<Long>> extractData(ResultSet rs) throws SQLException {
            Map<Long, Set<Long>> data = new LinkedHashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("filmId");
                data.putIfAbsent(filmId, new HashSet<>());
                Long userId = rs.getLong("userId");
                data.get(filmId).add(userId);
            }
            return data;
        }
    }

    public static class FilmGenreExtractor implements ResultSetExtractor<Map<Long, LinkedHashSet<Long>>> {
        @Override
        public Map<Long, LinkedHashSet<Long>> extractData(ResultSet rs) throws SQLException {
            Map<Long, LinkedHashSet<Long>> data = new LinkedHashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("filmId");
                data.putIfAbsent(filmId, new LinkedHashSet<>());
                Long genreId = rs.getLong("genreId");
                data.get(filmId).add(genreId);
            }
            return data;
        }
    }

    public static class FilmRatingExtractor implements ResultSetExtractor<Map<Long, Long>> {
        @Override
        public Map<Long, Long> extractData(ResultSet rs) throws SQLException {
            Map<Long, Long> data = new HashMap<>();
            while (rs.next()) {
                Long id = rs.getLong("id");
                data.putIfAbsent(id, 0L);
                Long ratingId = rs.getLong("ratingId");
                data.put(id, ratingId);
            }
            return data;
        }
    }

    @Override
    public List<Film> findAll() {
        log.info(LOG_GET_REQUEST);

        String sqlQuery = "SELECT f.id AS filmId, f.name, f.description, f.releaseDate, f.duration, " +
                "fg.genreId, g.name AS genreName, lu.userId AS likedUserId, fr.ratingId " +
                "FROM film f " +
                "LEFT JOIN filmGenre fg ON f.id = fg.filmId " +
                "LEFT JOIN genre g ON fg.genreId = g.id " +
                "LEFT JOIN likedUsers lu ON f.id = lu.filmId " +
                "LEFT JOIN filmRating fr ON f.id = fr.filmId";

        Map<Long, Film> filmMap = new HashMap<>();
        jdbcTemplate.query(sqlQuery, rs -> {
            Long filmId = rs.getLong("filmId");
            Film film = filmMap.get(filmId);
            if (film == null) {
                film = Film.builder()
                        .id(filmId)
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .releaseDate(rs.getDate("releaseDate").toLocalDate())
                        .duration(rs.getInt("duration"))
                        .likedUsers(new HashSet<>())
                        .genres(new LinkedHashSet<>())
                        .mpa(rs.getLong("ratingId"))
                        .build();
                filmMap.put(filmId, film);
            }

            Long genreId = rs.getLong("genreId");
            if (genreId > 0) {
                film.getGenres().add(Genre.of(genreId, rs.getString("genreName")).getId());
            }

            Long likedUserId = rs.getLong("likedUserId");
            if (likedUserId > 0) {
                film.getLikedUsers().add(likedUserId);
            }
        });

        return new ArrayList<>(filmMap.values());
    }

    @Override
    public FilmRequest findById(Long id) {
        log.info(LOG_GET_REQUEST);
        if (id == null || id == 0) {
            logAndThrowConditionsNotMetException(ERROR_NULL_ID);
        }

        // Основной запрос с JOIN для получения всех данных о фильме
        String sqlQuery = "SELECT f.id AS filmId, f.name, f.description, f.releaseDate, f.duration, " +
                "fg.genreId, g.name AS genreName, lu.userId AS likedUserId, fr.ratingId, r.rating AS ratingName " +
                "FROM film f " +
                "LEFT JOIN filmGenre fg ON f.id = fg.filmId " +
                "LEFT JOIN genre g ON fg.genreId = g.id " +
                "LEFT JOIN likedUsers lu ON f.id = lu.filmId " +
                "LEFT JOIN filmRating fr ON f.id = fr.filmId " +
                "LEFT JOIN filmrating r ON fr.ratingId = r.id " +
                "WHERE f.id = ?";

        // Используем ResultSetExtractor для обработки результата
        FilmRequest filmRequest = jdbcTemplate.query(sqlQuery, rs -> {
            FilmRequest.FilmRequestBuilder filmRequestBuilder = null;
            Set<Long> likedUsers = new HashSet<>();
            LinkedHashSet<Genre> genres = new LinkedHashSet<>();
            Mpa mpa = null;

            while (rs.next()) {
                if (filmRequestBuilder == null) {
                    // Создаем объект FilmRequest на основе данных из первой строки
                    filmRequestBuilder = FilmRequest.builder()
                            .id(rs.getLong("filmId"))
                            .name(rs.getString("name"))
                            .description(rs.getString("description"))
                            .releaseDate(rs.getDate("releaseDate").toLocalDate())
                            .duration(rs.getInt("duration"))
                            .mpa(Mpa.of(rs.getLong("ratingId"), rs.getString("ratingName")));
                }

                // Собираем лайки
                Long likedUserId = rs.getLong("likedUserId");
                if (likedUserId > 0) {
                    likedUsers.add(likedUserId);
                }

                // Собираем жанры
                Long genreId = rs.getLong("genreId");
                if (genreId > 0) {
                    genres.add(Genre.of(genreId, rs.getString("genreName")));
                }
            }

            if (filmRequestBuilder == null) {
                // Если фильм не найден, выбрасываем исключение
                logAndThrowNotFoundException(id.toString(), ERROR_FILM_NOT_FOUND);
            }

            // Устанавливаем лайки и жанры
            return filmRequestBuilder
                    .likedUsers(likedUsers)
                    .genres(genres)
                    .build();
        }, id);

        return filmRequest;
    }

    @Override
    public FilmRequest create(@Valid Buffer buffer) {
        log.info(LOG_CREATE_REQUEST);
        validateBuffer(buffer);

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("film").usingGeneratedKeyColumns("id");
        Long filmId = simpleJdbcInsert.executeAndReturnKey(buffer.toMapBuffer()).longValue();

        Map<Long, String> genre = jdbcTemplate.query(SQL_SELECT_GENRES, new GenreExtractor());
        Map<Long, String> rating = jdbcTemplate.query(SQL_SELECT_RATINGS, new RatingNameExtractor());

        LinkedHashSet<Genre> genres = processGenres(buffer.getGenres(), filmId, genre);
        updateFilmRating(buffer.getMpa(), filmId);

        return FilmRequest.of(filmId, buffer.getName(), buffer.getDescription(), buffer.getReleaseDate(), buffer.getDuration(), new HashSet<>(), Mpa.of(buffer.getMpa(), rating.get(buffer.getMpa())), genres);
    }

    @Override
    public FilmRequest update(@Valid Buffer newFilm) {
        log.info(LOG_UPDATE_REQUEST);
        if (newFilm.getId() == null) {
            logAndThrowConditionsNotMetException("Id должен быть указан");
        }

        FilmRequest oldFilm = findById(newFilm.getId());
        validateBuffer(newFilm);

        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());

        Map<Long, String> genre = jdbcTemplate.query(SQL_SELECT_GENRES, new GenreExtractor());
        Map<Long, String> rating = jdbcTemplate.query(SQL_SELECT_RATINGS, new RatingNameExtractor());

        LinkedHashSet<Genre> genres = processGenres(newFilm.getGenres(), oldFilm.getId(), genre);
        updateFilmRating(newFilm.getMpa(), oldFilm.getId());

        jdbcTemplate.update(SQL_UPDATE_FILM, oldFilm.getName(), oldFilm.getDescription(), oldFilm.getReleaseDate(),
                oldFilm.getDuration(), oldFilm.getMpa().getId(), oldFilm.getId());

        return FilmRequest.of(oldFilm.getId(), oldFilm.getName(), oldFilm.getDescription(), oldFilm.getReleaseDate(),
                oldFilm.getDuration(), new HashSet<>(), Mpa.of(newFilm.getMpa(), rating.get(newFilm.getMpa())), genres);
    }

    private void validateBuffer(Buffer buffer) {
        if (buffer.getName() == null || buffer.getName().isBlank()) {
            logAndThrowConditionsNotMetException(ERROR_EMPTY_NAME);
        }

        if (buffer.getDescription().length() > 200) {
            logAndThrowConditionsNotMetException(ERROR_DESCRIPTION_LENGTH);
        }

        if (buffer.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            logAndThrowConditionsNotMetException(ERROR_RELEASE_DATE);
        }

        if (buffer.getDuration() == null || buffer.getDuration() <= 0) {
            logAndThrowConditionsNotMetException(ERROR_DURATION);
        }

        if (!(buffer.getMpa() > 0 && buffer.getMpa() < 6)) {
            logAndThrowNotFoundException(buffer.getMpa().toString(), ERROR_INVALID_RATING);
        }
    }

    private LinkedHashSet<Genre> processGenres(List<String> genres, Long filmId, Map<Long, String> genreMap) {
        LinkedHashSet<Genre> result = new LinkedHashSet<>();
        if (genres == null || genres.equals(List.of("нет жанра"))) {
            return result;
        }

        for (String genreIdStr : genres) {
            Long genreId = Long.parseLong(genreIdStr);
            if (!(genreId > 0 && genreId < 7)) {
                logAndThrowNotFoundException(genreId.toString(), ERROR_INVALID_GENRE);
            }
            jdbcTemplate.update(SQL_INSERT_FILM_GENRE, filmId, genreId);
            result.add(Genre.of(genreId, genreMap.get(genreId)));
        }
        return result;
    }

    private void updateFilmRating(Long mpaId, Long filmId) {
        jdbcTemplate.update(SQL_UPDATE_FILM_RATING, mpaId, filmId);
    }

    private void logAndThrowConditionsNotMetException(String message) {
        log.error("Exception", new ConditionsNotMetException(message));
        throw new ConditionsNotMetException(message);
    }

    private void logAndThrowNotFoundException(String value, String message) {
        log.error("Exception", new NotFoundException(message));
        throw new NotFoundException(message);
    }
}