package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FilmService {

    private static final Map<Long, Film> films = new HashMap<>();

    public Collection<Film> getFilms() {
        log.info("Возвращается список фильмов...");
        return films.values();
    }

    public Film createFilm(@Valid Film film) throws ValidationException {
        log.info("Создается фильм: {}", film);
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    public Film update(@Valid Film newFilm) {
        log.info("Обновляется фильм: {}", newFilm);
        validateFilmId(newFilm.getId());

        Film oldFilm = films.get(newFilm.getId());
        if (oldFilm == null) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, newFilm.getId()));
        }

        updateFilmDetails(oldFilm, newFilm);
        return oldFilm;
    }

    private long getNextId() {
        return films.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    private void validateFilmId(Long id) {
        if (id == null) {
            logAndThrow(ExceptionMessages.FILM_ID_CANNOT_BE_NULL);
        }
    }

    private void validateFilm(Film film) throws ValidationException {
        if (film.getName() == null || film.getName().isBlank()) {
            logAndThrow(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            logAndThrow(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            logAndThrow(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }
        if (film.getDuration() <= 0) {
            logAndThrow(ExceptionMessages.FILM_DURATION_INVALID);
        }
    }

    private void logAndThrow(String message) throws ValidationException {
        log.error(message);
        throw new ValidationException(message);
    }

    private void updateFilmDetails(Film oldFilm, Film newFilm) {
        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());
    }
}