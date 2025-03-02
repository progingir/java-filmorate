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
    private final String selectAllGenresQuery = "select id, name from genre";
    private final String selectGenreByIdQuery = "select id, name from genre where id = ?";
    private final String selectAllRatingsQuery = "select id, rating from filmrating";
    private final String selectRatingByIdQuery = "select id, rating from filmrating where id = ?";


    @Override
    public FilmRequest addLike(Long idUser, Long idFilm) {
        log.info("Обработка Post-запроса...");
        if (userStorage.findById(idUser) != null && filmStorage.findById(idFilm) != null) {
            Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(selectLikedUsersQuery, new FilmDbStorage.LikedUsersExtractor());
            if (likedUsers.get(idFilm) != null && likedUsers.get(idFilm).contains(idUser)) {
                log.error("Exception", new ConditionsNotMetException(idUser.toString()));
                throw new ConditionsNotMetException(idUser.toString());
            } else {
                jdbcTemplate.update(insertLikeQuery, idFilm, idUser);
            }
        }
        FilmRequest film = filmStorage.findById(idFilm);
        LinkedHashSet genres = new LinkedHashSet<>();
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(selectFilmGenresQuery, new FilmDbStorage.FilmGenreExtractor(), film.getId());
        if (!filmGenre.isEmpty()) {
            for (Long g : filmGenre.get(film.getId()))
                genres.add(g);
        }
        return FilmRequest.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), film.getMpa(), genres);
    }

    @Override
    public FilmRequest delLike(Long idUser, Long idFilm) {
        log.info("Обработка Del-запроса...");
        if (userStorage.findById(idUser) != null && filmStorage.findById(idFilm) != null) {
            Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(selectLikedUsersQuery, new FilmDbStorage.LikedUsersExtractor());
            if (likedUsers.get(idFilm) != null && !likedUsers.get(idFilm).contains(idUser)) {
                log.error("Exception", new ConditionsNotMetException(idUser.toString()));
                throw new ConditionsNotMetException(idUser.toString());
            } else jdbcTemplate.update(deleteLikeQuery, idFilm, idUser);
        }
        FilmRequest film = filmStorage.findById(idFilm);
        LinkedHashSet genres = new LinkedHashSet<>();
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(selectFilmGenresQuery, new FilmDbStorage.FilmGenreExtractor(), film.getId());
        if (!filmGenre.isEmpty()) {
            for (Long g : filmGenre.get(film.getId()))
                genres.add(g);
        }
        return FilmRequest.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), film.getMpa(), genres);
    }

    public List<Film> viewRaiting(Long count) {
        return List.of();
    }

    public LinkedHashSet<FilmRequest> viewRating(Long count) {
        log.info("Обработка Get-запроса...");
        LinkedHashMap<Long, Long> likedUsers = jdbcTemplate.query(selectTopFilmsQuery, new TopLikedUsersExtractor());
        LinkedHashSet<FilmRequest> films = new LinkedHashSet<>();
        if (likedUsers == null) {
            log.error("Exception", new NotFoundException("Список фильмов с рейтингом пуст."));
            throw new NotFoundException("Список фильмов с рейтингом пуст.");
        } else {
            LinkedHashSet genres = new LinkedHashSet<>();
            for (Long l : likedUsers.keySet()) {
                Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(selectFilmGenresQuery, new FilmDbStorage.FilmGenreExtractor(), filmStorage.findById(l).getId());
                if (!filmGenre.isEmpty()) {
                    for (Long g : filmGenre.get(filmStorage.findById(l).getId()))
                        genres.add(g);
                }
                films.add(FilmRequest.of(filmStorage.findById(l).getId(), filmStorage.findById(l).getName(), filmStorage.findById(l).getDescription(), filmStorage.findById(l).getReleaseDate(), filmStorage.findById(l).getDuration(), new HashSet<>(), filmStorage.findById(l).getMpa(), genres));
            }
        }
        return films;
    }

    public List<GenreConstant> viewGenre() {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(selectAllGenresQuery, new GenreExtractor());
        List<GenreConstant> genreConstant = new ArrayList<>();
        for (Long l : genre.keySet())
            genreConstant.add(GenreConstant.of(l, genre.get(l)));
        return genreConstant;
    }

    public GenreConstant viewGenreName(Long id) {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(selectGenreByIdQuery, new GenreExtractor(), id);
        if (id < 0 || id > 7) {
            log.error("Exception", new NotFoundException("Жанра с указанным идентификатором не существует."));
            throw new NotFoundException("Жанра с указанным идентификатором не существует.");
        } else return GenreConstant.of(id, genre.get(id));
    }

    public List<Mpa> viewFilmsRating() {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(selectAllRatingsQuery, new RatingNameExtractor());
        List<Mpa> mpaConstant = new ArrayList<>();
        for (Long l : genre.keySet())
            mpaConstant.add(Mpa.of(l, genre.get(l)));
        return mpaConstant;
    }

    public Mpa viewRatingName(Long id) {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(selectRatingByIdQuery, new RatingNameExtractor(), id);
        if (id < 0 || id > 6) {
            log.error("Exception", new NotFoundException("Рейтинг с указанным идентификатором не существует."));
            throw new NotFoundException("Рейтинг с указанным идентификатором не существует.");
        } else return Mpa.of(id, genre.get(id));
    }
}