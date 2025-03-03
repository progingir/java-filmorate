package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FilmResponse;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

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
        log.info("Обработка Get-запроса для получения топ {} фильмов...", count);

        if (count == null || count <= 0) {
            log.error("Параметр count должен быть положительным числом");
            throw new ConditionsNotMetException("Количество фильмов должно быть положительным числом");
        }

        String limitedTopFilmsQuery = "SELECT f.id as name, COUNT(l.userId) as coun " +
                "FROM likedUsers as l " +
                "LEFT OUTER JOIN film AS f ON l.filmId = f.id " +
                "GROUP BY f.name " +
                "ORDER BY COUNT(l.userId) DESC " +
                "LIMIT ?";

        LinkedHashMap<Long, Long> likedUsers = jdbcTemplate.query(
                limitedTopFilmsQuery,
                new TopLikedUsersExtractor(),
                count
        );

        LinkedHashSet<FilmResponse> films = new LinkedHashSet<>();
        if (likedUsers == null || likedUsers.isEmpty()) {
            log.error("Список фильмов с рейтингом пуст.");
            throw new NotFoundException("Список фильмов с рейтингом пуст.");
        } else {
            for (Long filmId : likedUsers.keySet()) {
                FilmResponse film = filmStorage.findById(filmId);
                LinkedHashSet<Genre> genres = new LinkedHashSet<>();

                Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(
                        selectFilmGenresQuery,
                        new FilmDbStorage.FilmGenreExtractor(),
                        filmId
                );

                if (!filmGenre.isEmpty() && filmGenre.get(filmId) != null) {
                    for (Long genreId : filmGenre.get(filmId)) {
                        genres.add(getGenreById(genreId));
                    }
                }

                films.add(FilmResponse.of(
                        film.getId(),
                        film.getName(),
                        film.getDescription(),
                        film.getReleaseDate(),
                        film.getDuration(),
                        new HashSet<>(),
                        film.getMpa(),
                        genres
                ));

                if (films.size() >= count) {
                    break;
                }
            }
        }
        return films;
    }

    private Genre getGenreById(Long genreId) {
        String genreQuery = "SELECT id, name FROM genres WHERE id = ?";
        return jdbcTemplate.queryForObject(
                genreQuery,
                (rs, rowNum) -> Genre.of(rs.getLong("id"), rs.getString("name")),
                genreId
        );
    }
}