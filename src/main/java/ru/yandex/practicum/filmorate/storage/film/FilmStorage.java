package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRequest;

import java.util.Collection;

public interface FilmStorage {

    Collection<Film> findAll();

    FilmRequest findById(Long id) throws NotFoundException;

    Film create(Film film);

    Film update(Film film) throws NotFoundException;
}