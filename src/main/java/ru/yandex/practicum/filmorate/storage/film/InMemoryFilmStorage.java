package ru.yandex.practicum.filmorate.storage.film;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ExceptionMessages;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.*;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private static final Map<Long, Film> films = new HashMap<>();

    @Override
    public Collection<Film> findAll() {
        log.info("Обработка Get-запроса...");
        return films.values();
    }

    @Override
    public Film findById(Long id) throws NotFoundException {
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
    public Film create(@Valid Film film) {
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(@Valid Film newFilm) throws NotFoundException {
        validateFilm(newFilm);
        Film oldFilm = films.get(newFilm.getId());
        if (oldFilm == null) {
            log.error(String.format(ExceptionMessages.FILM_NOT_FOUND, newFilm.getId()));
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, newFilm.getId()));
        }
        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());
        return oldFilm;
    }

    public void addLike(Long filmId, Long userId) throws NotFoundException {
        Film film = findById(filmId);
        film.getLikedUsers().add(userId);
        log.info("Пользователь с ID = {} поставил лайк фильму с ID = {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) throws NotFoundException {
        Film film = findById(filmId);
        film.getLikedUsers().remove(userId);
        log.info("Пользователь с ID = {} удалил лайк с фильма с ID = {}", userId, filmId);
    }

    public List<Film> getTopFilms(int count) {
        log.info("Получение топ-{} фильмов по количеству лайков", count);
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikedUsers().size(), f1.getLikedUsers().size()))
                .limit(count)
                .toList();
    }

    private long getNextId() {
        return films.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    private void validateFilm(Film film) throws ValidationException {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
            throw new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
            throw new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }
        if (film.getReleaseDate().isBefore(ChronoLocalDate.from(LocalDateTime.of(1895, 12, 28, 0, 0, 0)))) {
            log.error(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
            throw new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }
        if (film.getDuration() <= 0) {
            log.error(ExceptionMessages.FILM_DURATION_INVALID);
            throw new ValidationException(ExceptionMessages.FILM_DURATION_INVALID);
        }
    }
}