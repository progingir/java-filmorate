package ru.yandex.practicum.filmorate.storage.film;

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
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.service.FilmInterface;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "TRACE")
@ConfigurationPropertiesScan
@Component
@Qualifier("FilmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final JdbcTemplate jdbcTemplate;
    private FilmInterface filmInterface;

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
    private final String sqlQuery12 = "insert into filmGenre(filmId, genreId) " + "values (?, ?)";
    private final String sqlQuery13 = "update film set " + "name = ?, description = ?, releaseDate = ?, duration = ?, ratingId = ? " + "where id = ?";
    private final String sqlQuery14 = "update film set " + "ratingId = ? " + "where id = ?";

    private Film mapRowToFilm(ResultSet resultSet, int rowNum) throws SQLException {
        return Film.builder().id(resultSet.getLong("id")).name(resultSet.getString("name")).description(resultSet.getString("description")).releaseDate(resultSet.getDate("releaseDate").toLocalDate()).duration(resultSet.getInt("duration")).build();
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
                data.putIfAbsent(id, Long.valueOf(0));
                Long ratingId = rs.getLong("ratingId");
                data.put(id, ratingId);
            }
            return data;
        }
    }

    @Override
    public List<Film> findAll() {
        log.info("Обработка Get-запроса...");
        List<Film> films = jdbcTemplate.query(sqlQuery1, this::mapRowToFilm);
        Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(sqlQuery2, new LikedUsersExtractor());
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(sqlQuery3, new FilmGenreExtractor());
        Map<Long, Long> filmRating = jdbcTemplate.query(sqlQuery4, new FilmRatingExtractor());
        for (Film film : films) {
            film.setLikedUsers(likedUsers.get(film.getId()));
            film.setGenres(filmGenre.get(film.getId()));
            film.setMpa(filmRating.get(film.getId()));
        }
        return films;
    }

    @Override
    public FilmRequest findById(Long id) throws ConditionsNotMetException, NotFoundException {
        log.info("Обработка Get-запроса...");
        if (id != 0 && !id.equals(null)) {
            try {
                jdbcTemplate.queryForObject(sqlQuery5, this::mapRowToFilm, id);
            } catch (DataAccessException e) {
                if (e != null) {
                    log.error("Exception", new NotFoundException(id.toString(), "Идентификатор фильма отсутствует в базе"));
                    throw new NotFoundException(id.toString(), "Идентификатор фильма отсутствует в базе");
                }
            }
            Film film = jdbcTemplate.queryForObject(sqlQuery5, this::mapRowToFilm, id);
            Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(sqlQuery6, new LikedUsersExtractor(), id);
            Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(sqlQuery7, new FilmGenreExtractor(), id);
            Map<Long, Long> filmRating = jdbcTemplate.query(sqlQuery8, new FilmRatingExtractor(), id);
            film.setLikedUsers(likedUsers.get(id));
            film.setGenres(filmGenre.get(id));
            Map<Long, String> genre = jdbcTemplate.query(sqlQuery9, new FilmService.GenreExtractor());
            Map<Long, String> rating = jdbcTemplate.query(sqlQuery10, new FilmService.RatingNameExtractor());
            LinkedHashSet<Genre> genres = new LinkedHashSet<>();
            if (!filmGenre.isEmpty()) {
                for (Long g : filmGenre.get(id))
                    genres.add(Genre.of(g, genre.get(g)));
            }
            film.setMpa(filmRating.get(id));
            return FilmRequest.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), Mpa.of(film.getMpa(), rating.get(film.getMpa())), genres);
        } else {
            log.error("Exception", new ConditionsNotMetException("Идентификатор фильма не может быть нулевой"));
            throw new ConditionsNotMetException("Идентификатор фильма не может быть нулевой");
        }
    }

    public FilmRequest create(@Valid Buffer buffer) throws ConditionsNotMetException, NullPointerException {
        log.info("Обработка Create-запроса...");
        if (buffer.getName() != null && !buffer.getName().isBlank() && !buffer.getName().equals("")) {
            if (buffer.getDescription().length() > 200) {
                log.error("Exception", new ConditionsNotMetException("Максимальная длина описания — 200 символов"));
                throw new ConditionsNotMetException("Максимальная длина описания — 200 символов");
            } else if (buffer.getReleaseDate().isBefore(ChronoLocalDate.from(LocalDateTime.of(1895, 12, 28, 0, 0, 0)))) {
                log.error("Exception", new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года"));
                throw new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года");
            } else if (buffer.getDuration() != null && buffer.getDuration() != 0) {
                if (buffer.getDuration() < 0) {
                    log.error("Exception", new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом"));
                    throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
                } else if (!(buffer.getMpa() > 0 && buffer.getMpa() < 6)) {
                    log.error("Exception", new NotFoundException(buffer.getMpa().toString(), "Некорректный рейтинг"));
                    throw new NotFoundException(buffer.getMpa().toString(), "Некорректный рейтинг");
                } else {
                    List<Long> genres;
                    LinkedHashSet<Genre> genres1 = new LinkedHashSet<>();
                    String sqlQuery;
                    SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("film").usingGeneratedKeyColumns("id");
                    Long f = simpleJdbcInsert.executeAndReturnKey(buffer.toMapBuffer()).longValue();
                    Map<Long, String> genre = jdbcTemplate.query(sqlQuery9, new FilmService.GenreExtractor());
                    Map<Long, String> rating = jdbcTemplate.query(sqlQuery10, new FilmService.RatingNameExtractor());
                    if (!buffer.getGenres().equals(List.of("нет жанра"))) {
                        genres = buffer.getGenres().stream().map(item -> Long.parseLong(item)).collect(Collectors.toList());
                        for (Long g : genres) {
                            if (!(g > 0 && g < 7)) {
                                log.error("Exception", new NotFoundException(g.toString(), "Некорректный жанр"));
                                throw new NotFoundException(g.toString(), "Некорректный жанр");
                            }
                        }
                        for (Long g : genres) {
                            jdbcTemplate.update(sqlQuery12, f, g);
                            genres1.add(Genre.of(g, genre.get(g)));
                        }
                    }
                    jdbcTemplate.update(sqlQuery14, buffer.getMpa(), f);

                    FilmRequest film = FilmRequest.of(f, buffer.getName(), buffer.getDescription(), buffer.getReleaseDate(), buffer.getDuration(), new HashSet<>(), Mpa.of(buffer.getMpa(), rating.get(buffer.getMpa())), genres1);
                    return film;
                }
            } else {
                log.error("Exception", new NullPointerException("Продолжительность фильма не может быть нулевой"));
                throw new NullPointerException("Продолжительность фильма не может быть нулевой");
            }
        } else {
            log.error("Exception", new ConditionsNotMetException("Название не может быть пустым"));
            throw new ConditionsNotMetException("Название не может быть пустым");
        }
    }

    public FilmRequest update(@Valid Buffer newFilm) throws ConditionsNotMetException, NotFoundException {
        log.info("Обработка Put-запроса...");
        if (newFilm.getId() == null) {
            log.error("Exception", new ConditionsNotMetException("Id должен быть указан"));
            throw new ConditionsNotMetException("Id должен быть указан");
        } else {
            FilmRequest oldFilm = findById(newFilm.getId());
            if (newFilm.getName() != null && !newFilm.getName().isBlank()) {
                oldFilm.setName(newFilm.getName());
                if (newFilm.getDescription().length() > 200) {
                    log.error("Exception", new ConditionsNotMetException("Максимальная длина описания — 200 символов"));
                    throw new ConditionsNotMetException("Максимальная длина описания — 200 символов");
                } else {
                    oldFilm.setDescription(newFilm.getDescription());
                    if (newFilm.getReleaseDate().isBefore(ChronoLocalDate.from(LocalDateTime.of(1895, 12, 28, 0, 0, 0)))) {
                        log.error("Exception", new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года"));
                        throw new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года");
                    } else {
                        oldFilm.setReleaseDate(newFilm.getReleaseDate());
                        if (newFilm.getDuration() != null && newFilm.getDuration() != 0) {
                            if (newFilm.getDuration() < 0) {
                                log.error("Exception", new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом"));
                                throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
                            } else {
                                oldFilm.setDuration(newFilm.getDuration());
                                if (!(newFilm.getMpa() > 0 && newFilm.getMpa() < 6)) {
                                    log.error("Exception", new NotFoundException(newFilm.getMpa().toString(), "Некорректный рейтинг"));
                                    throw new NotFoundException(newFilm.getMpa().toString(), "Некорректный рейтинг");
                                }
                                LinkedHashSet<Genre> genres1 = new LinkedHashSet<>();
                                Map<Long, String> genre = jdbcTemplate.query(sqlQuery9, new FilmService.GenreExtractor());
                                Map<Long, String> rating = jdbcTemplate.query(sqlQuery10, new FilmService.RatingNameExtractor());
                                if (!newFilm.getGenres().equals(List.of("нет жанра"))) {
                                    List<Long> genres = newFilm.getGenres().stream().map(item -> Long.parseLong(item)).collect(Collectors.toList());
                                    for (Long g : genres) {
                                        if (!(g > 0 && g < 7)) {
                                            log.error("Exception", new NotFoundException(g.toString(), "Некорректный жанр"));
                                            throw new NotFoundException(g.toString(), "Некорректный жанр");
                                        }
                                    }
                                    jdbcTemplate.update(sqlQuery11, oldFilm.getId());
                                    for (Long g : genres) {
                                        jdbcTemplate.update(sqlQuery12, oldFilm.getId(), g);
                                        genres1.add(Genre.of(g, genre.get(g)));
                                    }
                                }
                                if (!oldFilm.getMpa().equals(newFilm.getMpa()) && newFilm.getMpa() > 0 && newFilm.getMpa() < 6)
                                    oldFilm.setMpa(Mpa.of(newFilm.getMpa(), rating.get(newFilm.getMpa())));
                                jdbcTemplate.update(sqlQuery13, oldFilm.getName(), oldFilm.getDescription(), oldFilm.getReleaseDate(), oldFilm.getDuration(), oldFilm.getMpa().getId(), oldFilm.getId());
                                return FilmRequest.of(oldFilm.getId(), oldFilm.getName(), oldFilm.getDescription(), oldFilm.getReleaseDate(), oldFilm.getDuration(), new HashSet<>(), oldFilm.getMpa(), genres1);
                            }
                        } else {
                            log.error("Exception", new NullPointerException("Продолжительность фильма не может быть нулевой"));
                            throw new NullPointerException("Продолжительность фильма не может быть нулевой");
                        }
                    }
                }
            } else {
                log.error("Exception", new ConditionsNotMetException("Название не может быть пустым"));
                throw new ConditionsNotMetException("Название не может быть пустым");
            }
        }
    }
}