package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Buffer;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRequest;

import java.util.List;

public interface FilmStorage {

    List<Film> findAll();

    FilmRequest findById(Long id);

    FilmRequest create(Buffer film);

    FilmRequest update(Buffer newFilm);
}