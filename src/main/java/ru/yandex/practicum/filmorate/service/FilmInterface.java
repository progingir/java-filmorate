package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.FilmRequest;

import java.util.LinkedHashSet;

public interface FilmInterface {
    FilmRequest addLike(Long idUser, Long idFilm);

    FilmRequest delLike(Long idUser, Long idFilm);

    LinkedHashSet<FilmRequest> viewRating(Long count);
}