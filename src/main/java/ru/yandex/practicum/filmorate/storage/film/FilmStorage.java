package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {

    Collection<Film> findAll();

    Film findById(Long id) throws NotFoundException;

    Film create(Film film);

    Film update(Film film) throws NotFoundException;

    Film addLike(Long filmId, Long userId) throws NotFoundException;

    Film removeLike(Long filmId, Long userId) throws NotFoundException;

    List<Film> getTopFilms(int count);
}