package ru.yandex.practicum.filmorate.storage.film;

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
        log.info("Processing Get-request...");
        return films.values();
    }

    @Override
    public Film findById(Long id) throws NotFoundException {
        if (id == null) {
            throw new ValidationException(ExceptionMessages.FILM_ID_CANNOT_BE_NULL);
        }
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, id));
        }
        return film;
    }

    @Override
    public Film create(Film film) throws ValidationException {
        validateFilm(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) throws NotFoundException, ValidationException {
        validateFilm(film);
        Film oldFilm = films.get(film.getId());
        if (oldFilm == null) {
            throw new NotFoundException(String.format(ExceptionMessages.FILM_NOT_FOUND, film.getId()));
        }
        oldFilm.setName(film.getName());
        oldFilm.setDescription(film.getDescription());
        oldFilm.setReleaseDate(film.getReleaseDate());
        oldFilm.setDuration(film.getDuration());
        return oldFilm;
    }

//    @Override
//    public Film addLike(Long filmId, Long userId) throws NotFoundException {
//        Film film = findById(filmId);
//        film.getLikedUsers().add(userId);
//        log.info("User with ID = {} liked the film with ID = {}", userId, filmId);
//        return film; // Возвращаем фильм с обновленным списком лайков
//    }

    @Override
    public Film addLike(Long filmId, Long userId) throws NotFoundException {
        Film film = films.get(filmId);
        if (film == null) {
            log.warn("Film with ID = {} not found", filmId);
            return null; // Возвращаем null, если фильм не найден
        }

        // Добавляем лайк, даже если пользователь не существует (так как у нас нет доступа к UserStorage)
        film.getLikedUsers().add(userId);
        log.info("User with ID = {} liked the film with ID = {}", userId, filmId);
        return film; // Возвращаем фильм с обновленным списком лайков
    }

    @Override
    public Film removeLike(Long filmId, Long userId) throws NotFoundException {
        Film film = findById(filmId);
        if (!film.getLikedUsers().contains(userId)) {
            throw new NotFoundException(String.format("User with ID = %d did not like the film with ID = %d", userId, filmId));
        }
        film.getLikedUsers().remove(userId);
        log.info("User with ID = {} unliked the film with ID = {}", userId, filmId);
        return film; // Возвращаем фильм с обновленным списком лайков
    }

    @Override
    public List<Film> getTopFilms(int count) {
        log.info("Getting top-{} films by number of likes", count);
        return films.values().stream()
                .sorted(Comparator.comparingInt(f -> -f.getLikedUsers().size()))
                .limit(count)
                .toList();
    }

    private long getNextId() {
        return films.keySet().stream().mapToLong(id -> id).max().orElse(0) + 1;
    }

    private void validateFilm(Film film) throws ValidationException {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException(ExceptionMessages.FILM_NAME_CANNOT_BE_EMPTY);
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException(ExceptionMessages.FILM_DESCRIPTION_TOO_LONG);
        }
        if (film.getReleaseDate().isBefore(ChronoLocalDate.from(LocalDateTime.of(1895, 12, 28, 0, 0, 0)))) {
            throw new ValidationException(ExceptionMessages.FILM_RELEASE_DATE_INVALID);
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException(ExceptionMessages.FILM_DURATION_INVALID);
        }
    }
}