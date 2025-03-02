package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;

@Service
@Slf4j(topic = "TRACE")
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class FilmService implements FilmInterface {
    @Autowired
    UserStorage userStorage;
    @Autowired
    FilmStorage filmStorage;
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
            throw new IllegalArgumentException("Count must be greater than zero.");
        }

        // Используем параметризованный запрос для безопасности
        String limitedSelectTopFilmsQuery = selectTopFilmsQuery + " LIMIT ?";
        LinkedHashMap<Long, Long> likedUsers = jdbcTemplate.query(limitedSelectTopFilmsQuery, new TopLikedUsersExtractor(), count);

        LinkedHashSet<FilmResponse> films = new LinkedHashSet<>();
        if (likedUsers == null || likedUsers.isEmpty()) {
            log.error("Список фильмов с рейтингом пуст.");
            throw new NotFoundException("Список фильмов с рейтингом пуст.");
        }

        // Получаем все жанры за один запрос
        Map<Long, Genre> allGenres = new HashMap<>();
        jdbcTemplate.query("SELECT id, name FROM genres", rs -> {
            allGenres.put(rs.getLong("id"), Genre.of(rs.getLong("id"), rs.getString("name")));
        });

        for (Long filmId : likedUsers.keySet()) {
            FilmResponse film = filmStorage.findById(filmId);
            if (film == null) {
                log.warn("Фильм с id {} не найден.", filmId);
                continue;
            }

            // Получаем жанры для текущего фильма
            Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(selectFilmGenresQuery,
                    new FilmDbStorage.FilmGenreExtractor(), film.getId());

            LinkedHashSet<Genre> genres = new LinkedHashSet<>();
            if (!filmGenre.isEmpty()) {
                for (Long genreId : filmGenre.get(film.getId())) {
                    Genre genre = allGenres.get(genreId);
                    if (genre != null) {
                        genres.add(genre);
                    }
                }
            }

            // Создаем объект FilmResponse
            films.add(FilmResponse.of(
                    film.getId(),
                    film.getName(),
                    film.getDescription(),
                    film.getReleaseDate(),
                    film.getDuration(),
                    new HashSet<>(), // Предполагаем, что это для liked users, настройте при необходимости
                    film.getMpa(),
                    genres
            ));
        }

        log.info("Найдено фильмов: {}", films.size());
        return films;
    }
}