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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private final String sqlQuery9 = "select id, name from genre";
    private final String sqlQuery10 = "select id, rating from filmrating";

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
        log.info("Обработка Get-запроса...");
        String sqlQuery1 = "select id, name, description, releaseDate, duration from film";
        List<Film> films = jdbcTemplate.query(sqlQuery1, this::mapRowToFilm);
        String sqlQuery2 = "select filmId, userId from likedUsers";
        Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(sqlQuery2, new LikedUsersExtractor());
        String sqlQuery3 = "select filmId, genreId from filmGenre";
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(sqlQuery3, new FilmGenreExtractor());
        String sqlQuery4 = "select id, ratingId from film";
        Map<Long, Long> filmRating = jdbcTemplate.query(sqlQuery4, new FilmRatingExtractor());
        for (Film film : films) {
            assert likedUsers != null;
            film.setLikedUsers(likedUsers.get(film.getId()));
            assert filmGenre != null;
            film.setGenres(filmGenre.get(film.getId()));
            assert filmRating != null;
            film.setMpa(filmRating.get(film.getId()));
        }
        return films;
    }

    @Override
    public FilmRequest findById(Long id) throws ConditionsNotMetException, NotFoundException {
        log.info("Обработка Get-запроса...");
        if (id == null || id == 0) {
            log.error("Exception", new ConditionsNotMetException("Идентификатор фильма не может быть нулевой"));
            throw new ConditionsNotMetException("Идентификатор фильма не может быть нулевой");
        }

        String sqlQuery5 = "select id, name, description, releaseDate, duration from film where id = ?";
        try {
            jdbcTemplate.queryForObject(sqlQuery5, this::mapRowToFilm, id);
        } catch (DataAccessException e) {
            log.error("Exception", new NotFoundException(id.toString(), "Идентификатор фильма отсутствует в базе"));
            throw new NotFoundException(id.toString(), "Идентификатор фильма отсутствует в базе");
        }

        Film film = jdbcTemplate.queryForObject(sqlQuery5, this::mapRowToFilm, id);
        String sqlQuery6 = "select filmId, userId from likedUsers where filmId = ?";
        Map<Long, Set<Long>> likedUsers = jdbcTemplate.query(sqlQuery6, new LikedUsersExtractor(), id);
        String sqlQuery7 = "select filmId, genreId from filmGenre where filmId = ?";
        Map<Long, LinkedHashSet<Long>> filmGenre = jdbcTemplate.query(sqlQuery7, new FilmGenreExtractor(), id);
        String sqlQuery8 = "select id, ratingId from film where id = ?";
        Map<Long, Long> filmRating = jdbcTemplate.query(sqlQuery8, new FilmRatingExtractor(), id);
        assert film != null;
        assert likedUsers != null;
        film.setLikedUsers(likedUsers.get(id));
        assert filmGenre != null;
        film.setGenres(filmGenre.get(id));
        Map<Long, String> genre = jdbcTemplate.query(sqlQuery9, new FilmService.GenreExtractor());
        Map<Long, String> rating = jdbcTemplate.query(sqlQuery10, new FilmService.RatingNameExtractor());
        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        if (!filmGenre.isEmpty()) {
            for (Long g : filmGenre.get(id)) {
                assert genre != null;
                genres.add(Genre.of(g, genre.get(g)));
            }
        }
        assert filmRating != null;
        film.setMpa(filmRating.get(id));
        return FilmRequest.of(film.getId(), film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), new HashSet<>(), Mpa.of(film.getMpa(), rating.get(film.getMpa())), genres);
    }

    @Override
    public FilmRequest create(@Valid Buffer buffer) throws ConditionsNotMetException, NullPointerException {
        log.info("Обработка Create-запроса...");
        validateBuffer(buffer);

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("film").usingGeneratedKeyColumns("id");
        Long filmId = simpleJdbcInsert.executeAndReturnKey(buffer.toMapBuffer()).longValue();

        Map<Long, String> genre = jdbcTemplate.query(sqlQuery9, new FilmService.GenreExtractor());
        Map<Long, String> rating = jdbcTemplate.query(sqlQuery10, new FilmService.RatingNameExtractor());

        LinkedHashSet<Genre> genres = processGenres(buffer.getGenres(), filmId, genre);
        updateFilmRating(buffer.getMpa(), filmId);

        return FilmRequest.of(filmId, buffer.getName(), buffer.getDescription(), buffer.getReleaseDate(), buffer.getDuration(), new HashSet<>(), Mpa.of(buffer.getMpa(), rating.get(buffer.getMpa())), genres);
    }

    @Override
    public FilmRequest update(@Valid Buffer newFilm) throws ConditionsNotMetException, NotFoundException {
        log.info("Обработка Put-запроса...");
        if (newFilm.getId() == null) {
            log.error("Exception", new ConditionsNotMetException("Id должен быть указан"));
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        FilmRequest oldFilm = findById(newFilm.getId());
        validateBuffer(newFilm);

        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());

        Map<Long, String> genre = jdbcTemplate.query(sqlQuery9, new FilmService.GenreExtractor());
        Map<Long, String> rating = jdbcTemplate.query(sqlQuery10, new FilmService.RatingNameExtractor());

        LinkedHashSet<Genre> genres = processGenres(newFilm.getGenres(), oldFilm.getId(), genre);
        updateFilmRating(newFilm.getMpa(), oldFilm.getId());

        String sqlQuery13 = "update film set name = ?, description = ?, releaseDate = ?, duration = ?, ratingId = ? where id = ?";
        jdbcTemplate.update(sqlQuery13, oldFilm.getName(), oldFilm.getDescription(), oldFilm.getReleaseDate(),
                oldFilm.getDuration(), oldFilm.getMpa().getId(), oldFilm.getId());

        return FilmRequest.of(oldFilm.getId(), oldFilm.getName(), oldFilm.getDescription(), oldFilm.getReleaseDate(),
                oldFilm.getDuration(), new HashSet<>(), Mpa.of(newFilm.getMpa(), rating.get(newFilm.getMpa())), genres);
    }

    private void validateBuffer(Buffer buffer) throws ConditionsNotMetException, NullPointerException {
        if (buffer.getName() == null || buffer.getName().isBlank()) {
            log.error("Exception", new ConditionsNotMetException("Название не может быть пустым"));
            throw new ConditionsNotMetException("Название не может быть пустым");
        }

        if (buffer.getDescription().length() > 200) {
            log.error("Exception", new ConditionsNotMetException("Максимальная длина описания — 200 символов"));
            throw new ConditionsNotMetException("Максимальная длина описания — 200 символов");
        }

        if (buffer.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.error("Exception", new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года"));
            throw new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года");
        }

        if (buffer.getDuration() == null || buffer.getDuration() <= 0) {
            log.error("Exception", new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом"));
            throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
        }

        if (!(buffer.getMpa() > 0 && buffer.getMpa() < 6)) {
            log.error("Exception", new NotFoundException(buffer.getMpa().toString(), "Некорректный рейтинг"));
            throw new NotFoundException(buffer.getMpa().toString(), "Некорректный рейтинг");
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
                log.error("Exception", new NotFoundException(genreId.toString(), "Некорректный жанр"));
                throw new NotFoundException(genreId.toString(), "Некорректный жанр");
            }
            String sqlQuery12 = "insert into filmGenre(filmId, genreId) values (?, ?)";
            jdbcTemplate.update(sqlQuery12, filmId, genreId);
            result.add(Genre.of(genreId, genreMap.get(genreId)));
        }
        return result;
    }

    private void updateFilmRating(Long mpaId, Long filmId) {
        String sqlQuery14 = "update film set ratingId = ? where id = ?";
        jdbcTemplate.update(sqlQuery14, mpaId, filmId);
    }
}