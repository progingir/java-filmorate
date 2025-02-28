package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    private final String sqlQuery1 = "select filmId, userId from likedUsers";
    private final String sqlQuery2 = "insert into likedUsers(filmId, userId) " + "values (?, ?)";
    private final String sqlQuery3 = "select filmId, genreId from filmGenre where filmId = ?";
    private final String sqlQuery4 = "delete from likedUsers where filmId = ? and userId = ?";
    private final String sqlQuery5 = "select f.id as name, COUNT(l.userId) as coun from likedUsers as l LEFT OUTER JOIN film AS f ON l.filmId = f.id GROUP BY f.name ORDER BY COUNT(l.userId) DESC LIMIT 10";
    private final String sqlQuery6 = "select id, name from genre";
    private final String sqlQuery7 = "select id, name from genre where id = ?";
    private final String sqlQuery8 = "select id, rating from filmrating";
    private final String sqlQuery9 = "select id, rating from filmrating where id = ?";

    public static class TopLikedUsersExtractor implements ResultSetExtractor<LinkedHashMap<Long, Long>> {
        @Override
        public LinkedHashMap<Long, Long> extractData(ResultSet rs) throws SQLException {
            LinkedHashMap<Long, Long> data = new LinkedHashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("name");
                Long likes = rs.getLong("coun");
                data.putIfAbsent(filmId, likes);
            }
            return data;
        }
    }

    @Override
    public FilmRequest addLike(Long idUser, Long idFilm) throws ConditionsNotMetException {
        log.info("Обработка Post-запроса...");
        if (userStorage.findById(idUser) != null && filmStorage.findById(idFilm) != null) {
            Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(sqlQuery1, new FilmDbStorage.LikedUsersExtractor());
            if (likedUsers.get(idFilm) != null && likedUsers.get(idFilm).contains(idUser)) {
                log.error("Exception", new ConditionsNotMetException(idUser.toString()));
                throw new ConditionsNotMetException(idUser.toString());
            } else {
                jdbcTemplate.update(sqlQuery2, idFilm, idUser);
            }
        }
        Film film = filmStorage.findById(idFilm);
        LinkedHashSet genres = new LinkedHashSet<>();
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(sqlQuery3, new FilmDbStorage.FilmGenreExtractor(), film.getId());
        if (!filmGenre.isEmpty()) {
            for (Long g : filmGenre.get(film.getId()))
                genres.add(g);
        }
        return FilmRequest.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), film.getMpa(), genres);
    }

    @Override
    public FilmRequest delLike(Long idUser, Long idFilm) throws ConditionsNotMetException {
        log.info("Обработка Del-запроса...");
        if (userStorage.findById(idUser) != null && filmStorage.findById(idFilm) != null) {
            Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(sqlQuery1, new FilmDbStorage.LikedUsersExtractor());
            if (likedUsers.get(idFilm) != null && !likedUsers.get(idFilm).contains(idUser)) {
                log.error("Exception", new ConditionsNotMetException(idUser.toString()));
                throw new ConditionsNotMetException(idUser.toString());
            } else jdbcTemplate.update(sqlQuery4, idFilm, idUser);
        }
        Film film = filmStorage.findById(idFilm);
        LinkedHashSet genres = new LinkedHashSet<>();
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(sqlQuery3, new FilmDbStorage.FilmGenreExtractor(), film.getId());
        if (!filmGenre.isEmpty()) {
            for (Long g : filmGenre.get(film.getId()))
                genres.add(g);
        }
        return FilmRequest.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), film.getMpa(), genres);
    }

    @Override
    public List<Film> viewRaiting(Long count) {
        return List.of();
    }

    public LinkedHashSet<FilmRequest> viewRating(Long count) throws NotFoundException {
        log.info("Обработка Get-запроса...");
        LinkedHashMap<Long, Long> likedUsers = jdbcTemplate.query(sqlQuery5, new TopLikedUsersExtractor());
        LinkedHashSet<FilmRequest> films = new LinkedHashSet<>();
        if (likedUsers == null) {
            log.error("Exception", new NotFoundException(count.toString(), "Список фильмов с рейтингом пуст."));
            throw new NotFoundException(count.toString(), "Список фильмов с рейтингом пуст.");
        } else {
            LinkedHashSet genres = new LinkedHashSet<>();
            for (Long l : likedUsers.keySet()) {
                Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(sqlQuery3, new FilmDbStorage.FilmGenreExtractor(), filmStorage.findById(l).getId());
                if (!filmGenre.isEmpty()) {
                    for (Long g : filmGenre.get(filmStorage.findById(l).getId()))
                        genres.add(g);
                }
                films.add(FilmRequest.of(filmStorage.findById(l).getId(), filmStorage.findById(l).getName(), filmStorage.findById(l).getDescription(), filmStorage.findById(l).getReleaseDate(), filmStorage.findById(l).getDuration(), new HashSet<>(), filmStorage.findById(l).getMpa(), genres));
            }
        }
        return films;
    }

    public List<GenreConstant> viewGenre() throws NotFoundException {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(sqlQuery6, new GenreExtractor());
        List<GenreConstant> genreConstant = new ArrayList<>();
        for (Long l : genre.keySet())
            genreConstant.add(GenreConstant.of(l, genre.get(l)));
        return genreConstant;
    }

    public static class GenreExtractor implements ResultSetExtractor<Map<Long, String>> {
        @Override
        public Map<Long, String> extractData(ResultSet rs) throws SQLException {
            Map<Long, String> data = new LinkedHashMap<>();
            while (rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("name");
                data.put(id, name);
            }
            return data;
        }
    }

    public GenreConstant viewGenreName(Long id) throws NotFoundException {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(sqlQuery7, new GenreExtractor(), id);
        if (id < 0 || id > 7) {
            log.error("Exception", new NotFoundException("NULL", "Жанра с указанным идентификатором не существует."));
            throw new NotFoundException("NULL", "Жанра с указанным идентификатором не существует.");
        } else return GenreConstant.of(id, genre.get(id));
    }

    public List<MpaConstant> viewFilmsRating() throws NotFoundException {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(sqlQuery8, new RatingNameExtractor());
        List<MpaConstant> mpaConstant = new ArrayList<>();
        for (Long l : genre.keySet())
            mpaConstant.add(MpaConstant.of(l, genre.get(l)));
        return mpaConstant;
    }

    public static class RatingNameExtractor implements ResultSetExtractor<Map<Long, String>> {
        @Override
        public Map<Long, String> extractData(ResultSet rs) throws SQLException {
            Map<Long, String> data = new HashMap<>();
            while (rs.next()) {
                Long id = rs.getLong("id");
                String rating = rs.getString("rating");
                data.put(id, rating);
            }
            return data;
        }
    }

    public MpaConstant viewRatingName(Long id) throws NotFoundException {
        log.info("Обработка Get-запроса...");
        Map<Long, String> genre = jdbcTemplate.query(sqlQuery9, new RatingNameExtractor(), id);
        if (id < 0 || id > 6) {
            log.error("Exception", new NotFoundException("NULL", "Рейтинг с указанным идентификатором не существует."));
            throw new NotFoundException("NULL", "Рейтинг с указанным идентификатором не существует.");
        } else return MpaConstant.of(id, genre.get(id));
    }
}