package ru.yandex.practicum.filmorate.storage.film;

import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@NoArgsConstructor
@Slf4j(topic = "TRACE")
@ConfigurationPropertiesScan
public class InMemoryFilmStorage implements FilmStorage {

    private static final Map<Long, Film> films = new HashMap<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Collection<Film> findAll() {
        log.info("Обработка Get-запроса...");
        return films.values();
    }

    @Override
    public Film findById(Long id) throws NotFoundException, ValidationException {
        log.info("Обработка Get-запроса...");
        if (id == null) {
            log.error(ExceptionMessages.FILM_ID_CANNOT_BE_NULL);
            throw new ValidationException(ExceptionMessages.FILM_ID_CANNOT_BE_NULL);
        }

        Film film = films.get(id);
        if (film == null) {
            log.error(String.format(ExceptionMessages.FILM_NOT_FOUND, id));
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, id));
        }
        return film;
    }

    @Override
    public Film create(@Valid Film film) throws ValidationException {
        log.info("Обработка Create-запроса...");
        if (film.getName() == null || film.getName().isBlank()) {
            log.error(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
            throw new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }
        if (film.getDescription().length() > 200) {
            log.error(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
            throw new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }
        if (film.getReleaseDate().isBefore(ChronoLocalDate.from(LocalDateTime.of(1895, 12, 28, 0, 0, 0)))) {
            log.error(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
            throw new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }
        if (film.getDuration() == 0 || film.getDuration() <= 0) {
            log.error(ExceptionMessages.FILM_DURATION_INVALID);
            throw new ValidationException(ExceptionMessages.FILM_DURATION_INVALID);
        }

        film.setId(getNextId());
        film.setLikedUsers(new HashSet<>());
        films.put(film.getId(), film);
        return film;
    }

    private long getNextId() {
        long currentMaxId = films.keySet().stream().mapToLong(id -> id).max().orElse(0L);
        return ++currentMaxId;
    }

    @Override
    public Film update(@Valid Film newFilm) throws NotFoundException, ValidationException {
        log.info("Обработка Put-запроса...");
        if (newFilm.getId() == null) {
            log.error(ExceptionMessages.ID_CANNOT_BE_NULL);
            throw new ValidationException(ExceptionMessages.ID_CANNOT_BE_NULL);
        }

        Film oldFilm = films.get(newFilm.getId());
        if (oldFilm == null) {
            log.error(String.format(ExceptionMessages.FILM_NOT_FOUND, newFilm.getId()));
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, newFilm.getId()));
        }

        if (newFilm.getName() == null || newFilm.getName().isBlank()) {
            log.error(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
            throw new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }
        if (newFilm.getDescription().length() > 200) {
            log.error(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
            throw new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }
        if (newFilm.getReleaseDate().isBefore(ChronoLocalDate.from(LocalDateTime.of(1895, 12, 28, 0, 0, 0)))) {
            log.error(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
            throw new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }
        if (newFilm.getDuration() == 0 || newFilm.getDuration() <= 0) {
            log.error(ExceptionMessages.FILM_DURATION_INVALID);
            throw new ValidationException(ExceptionMessages.FILM_DURATION_INVALID);
        }

        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());
        return oldFilm;
    }
}