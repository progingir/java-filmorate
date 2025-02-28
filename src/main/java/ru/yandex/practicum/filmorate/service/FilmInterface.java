package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRequest;

import java.util.List;

public interface FilmInterface {
    FilmRequest addLike(Long idUser, Long idFilm);

    FilmRequest delLike(Long idUser, Long idFilm);

    List<Film> viewRaiting(Long count);
}