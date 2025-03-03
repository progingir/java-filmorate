package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FilmResponse;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j(topic = "TRACE")
@RequiredArgsConstructor
public class FilmService implements FilmInterface {

    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final JdbcTemplate jdbcTemplate;

    // SQL-запросы
    private final String selectLikedUsersQuery = "select filmId, userId from likedUsers";
    private final String insertLikeQuery = "insert into likedUsers(filmId, userId) values (?, ?)";
    private final String selectFilmGenresQuery = "select filmId, genreId from filmGenre where filmId = ?";
    private final String deleteLikeQuery = "delete from likedUsers where filmId = ? and userId = ?";
    private final String selectTopFilmsQuery = "select f.id as name, COUNT(l.userId) as coun from likedUsers as l LEFT OUTER JOIN film AS f ON l.filmId = f.id GROUP BY f.name ORDER BY COUNT(l.userId) DESC LIMIT 10";

    @Override
    public FilmResponse addLike(Long idUser, Long idFilm) {
        log.info("Обработка Post-запроса...");
        if (userStorage.findById(idUser) != null && filmStorage.findById(idFilm) != null) {
            Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(selectLikedUsersQuery, new FilmDbStorage.LikedUsersExtractor());
            if (likedUsers.get(idFilm) != null && likedUsers.get(idFilm).contains(idUser)) {
                log.error("Пользователь с ID {} уже поставил лайк фильму с ID {}", idUser, idFilm);
                throw new ConditionsNotMetException("Пользователь с ID " + idUser + " уже поставил лайк фильму с ID " + idFilm);
            } else {
                jdbcTemplate.update(insertLikeQuery, idFilm, idUser);
            }
        }
        FilmResponse film = filmStorage.findById(idFilm);
        LinkedHashSet genres = new LinkedHashSet<>();
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(selectFilmGenresQuery, new FilmDbStorage.FilmGenreExtractor(), film.getId());
        if (!filmGenre.isEmpty()) {
            for (Long g : filmGenre.get(film.getId()))
                genres.add(g);
        }
        return FilmResponse.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), film.getMpa(), genres);
    }

    @Override
    public FilmResponse delLike(Long idUser, Long idFilm) {
        log.info("Обработка Del-запроса...");
        if (userStorage.findById(idUser) != null && filmStorage.findById(idFilm) != null) {
            Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(selectLikedUsersQuery, new FilmDbStorage.LikedUsersExtractor());
            if (likedUsers.get(idFilm) != null && !likedUsers.get(idFilm).contains(idUser)) {
                log.error("Пользователь с ID {} не ставил лайк фильму с ID {}", idUser, idFilm);
                throw new ConditionsNotMetException("Пользователь с ID " + idUser + " не ставил лайк фильму с ID " + idFilm);
            } else {
                jdbcTemplate.update(deleteLikeQuery, idFilm, idUser);
            }
        }
        FilmResponse film = filmStorage.findById(idFilm);
        LinkedHashSet genres = new LinkedHashSet<>();
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(selectFilmGenresQuery, new FilmDbStorage.FilmGenreExtractor(), film.getId());
        if (!filmGenre.isEmpty()) {
            for (Long g : filmGenre.get(film.getId()))
                genres.add(g);
        }
        return FilmResponse.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), film.getMpa(), genres);
    }

    public LinkedHashSet<FilmResponse> viewRating(Long count) {
        log.info("Обработка Get-запроса...");

        if (count == null || count <= 0) {
            log.error("Некорректное значение параметра count: {}", count);
            throw new IllegalArgumentException("Параметр count должен быть положительным числом.");
        }

        String query = """
            SELECT f.id AS film_id,
                   f.name AS film_name,
                   f.description AS film_description,
                   f.release_date AS film_release_date,
                   f.duration AS film_duration,
                   f.mpa_id AS film_mpa_id,
                   m.name AS mpa_name,
                   g.id AS genre_id,
                   g.name AS genre_name,
                   COUNT(l.user_id) OVER (PARTITION BY f.id) AS likes_count
            FROM films f
            LEFT JOIN film_genres fg ON f.id = fg.film_id
            LEFT JOIN genres g ON fg.genre_id = g.id
            LEFT JOIN likes l ON f.id = l.film_id
            LEFT JOIN mpa m ON f.mpa_id = m.id
            ORDER BY likes_count DESC, f.id ASC
            LIMIT ?
            """;

        try {
            Map<Long, FilmResponse> filmMap = new LinkedHashMap<>();
            jdbcTemplate.query(query, (rs, rowNum) -> {
                Long filmId = rs.getLong("film_id");
                String name = rs.getString("film_name");
                String description = rs.getString("film_description");
                LocalDate releaseDate = rs.getDate("film_release_date").toLocalDate();
                Integer duration = rs.getInt("film_duration");
                Long mpaId = rs.getLong("film_mpa_id");
                String mpaName = rs.getString("mpa_name");
                Long genreId = rs.getObject("genre_id", Long.class);
                String genreName = rs.getString("genre_name");

                // Создаем объект Mpa
                Mpa mpa = Mpa.of(mpaId, mpaName);

                // Создаем или обновляем фильм
                FilmResponse film = filmMap.get(filmId);
                if (film == null) {
                    LinkedHashSet<Genre> genres = new LinkedHashSet<>();
                    if (genreId != null && genreName != null) {
                        genres.add(Genre.of(genreId, genreName));
                    }
                    film = FilmResponse.of(filmId, name, description, releaseDate, duration, null, mpa, genres);
                    filmMap.put(filmId, film);
                } else {
                    if (genreId != null && genreName != null) {
                        film.getGenres().add(Genre.of(genreId, genreName));
                    }
                }
                return null;
            }, count);

            return new LinkedHashSet<>(filmMap.values());
        } catch (EmptyResultDataAccessException e) {
            log.error("Список фильмов с рейтингом пуст.");
            throw new NotFoundException("Список фильмов с рейтингом пуст.");
        }
    }
}