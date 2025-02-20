package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {

    // Получение всех фильмов
    Collection<Film> findAll();

    // Получение фильма по ID
    Film findById(Long id) throws NotFoundException;

    // Создание нового фильма
    Film create(Film film);

    // Обновление существующего фильма
    Film update(Film film) throws NotFoundException;

    // Добавление лайка к фильму
    void addLike(Long filmId, Long userId) throws NotFoundException;

    // Удаление лайка из фильма
    void removeLike(Long filmId, Long userId) throws NotFoundException;

    // Получение топ-N популярных фильмов
    List<Film> getTopFilms(int count);
}