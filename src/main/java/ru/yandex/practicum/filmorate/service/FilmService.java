package ru.yandex.practicum.filmorate.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
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

    public Film getFilmById(Long id) throws ValidationException {
        log.info("Выводится фильм с айди {}", id);
        validateFilmId(id);
        Film film = films.get(id);
        if (film == null) {
            log.error("Exception", new ValidationException(String.format(ExceptionMessages.FILM_NOT_FOUND, id)));
            throw new ValidationException(String.format(ExceptionMessages.FILM_NOT_FOUND, id));
        }
        return film;
    }

    public Film createFilm(@Valid Film film) throws ValidationException {
        log.info("Создается фильм: {}", film);
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    public Film update(@Valid Film newFilm) throws ValidationException, NotFoundException {
        log.info("Обновляется фильм: {}", newFilm);
        validateFilmId(newFilm.getId());

        Film oldFilm = films.get(newFilm.getId());
        if (oldFilm == null) {
            log.error("Exception", new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, newFilm.getId())));
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, newFilm.getId()));
        }

        updateFilmDetails(oldFilm, newFilm);
        return oldFilm;
    }

    private long getNextId() {
        return films.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    private void validateFilmId(Long id) throws ValidationException {
        if (id == null) {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_ID_CANNOT_BE_NULL));
            throw new ValidationException(ExceptionMessages.FILM_ID_CANNOT_BE_NULL);
        }
    }

    private void validateFilm(Film film) throws ValidationException {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY));
            throw new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }
        if (film.getDescription().length() > 200) {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG));
            throw new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID));
            throw new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }
        if (film.getDuration() <= 0) {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_DURATION_INVALID));
            throw new ValidationException(ExceptionMessages.FILM_DURATION_INVALID);
        }
    }

    private void updateFilmDetails(Film oldFilm, Film newFilm) throws ValidationException {
        if (newFilm.getName() != null && !newFilm.getName().isBlank()) {
            oldFilm.setName(newFilm.getName());
        } else {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY));
            throw new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }

        if (newFilm.getDescription().length() <= 200) {
            oldFilm.setDescription(newFilm.getDescription());
        } else {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG));
            throw new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }

        if (!newFilm.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
        } else {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID));
            throw new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }

        if (newFilm.getDuration() > 0) {
            oldFilm.setDuration(newFilm.getDuration());
        } else {
            log.error("Exception", new ValidationException(ExceptionMessages.FILM_DURATION_INVALID));
            throw new ValidationException(ExceptionMessages.FILM_DURATION_INVALID);
        }
    }
}