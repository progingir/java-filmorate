package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Buffer;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRequest;

import java.util.List;

public interface FilmStorage {

    public List<Film> findAll();

    public FilmRequest findById(Long id);

    public FilmRequest create(Buffer film);

    public FilmRequest update(Buffer newFilm);
}