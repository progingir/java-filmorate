package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;

    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public Collection<Film> getFilms() {
        log.info("Возвращается список фильмов...");
        return filmStorage.findAll();
    }

    public Film createFilm(Film film) {
        log.info("Создается фильм: {}", film);
        return filmStorage.create(film);
    }

    public Film update(Film film) throws NotFoundException {
        log.info("Обновляется фильм: {}", film);
        return filmStorage.update(film);
    }

    public Film getFilmById(Long id) throws NotFoundException {
        return filmStorage.findById(id);
    }

    public void addLike(Long filmId, Long userId) throws NotFoundException {
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) throws NotFoundException {
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getTopFilms(int count) {
        return filmStorage.getTopFilms(count);
    }
}