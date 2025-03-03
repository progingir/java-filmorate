package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Buffer;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmResponse;
import ru.yandex.practicum.filmorate.service.FilmInterface;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/films")
public class FilmController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DEFAULT_GENRE = "нет жанра";

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmInterface filmInterface;

    @Autowired
    public FilmController(
            FilmStorage filmStorage,
            UserStorage userStorage,
            FilmInterface filmInterface
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmInterface = filmInterface;
    }

    @GetMapping
    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    @GetMapping("/{id}")
    public FilmResponse findById(@PathVariable("id") Long id) {
        return filmStorage.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmResponse create(@Valid @RequestBody FilmResponse filmRequest) {
        Buffer buffer = parseFilmRequestToBuffer(filmRequest);
        return filmStorage.create(buffer);
    }

    @PutMapping
    public FilmResponse update(@Valid @RequestBody FilmResponse filmRequest) {
        Buffer buffer = parseFilmRequestToBuffer(filmRequest);
        return filmStorage.update(buffer);
    }

    @PutMapping("/{id}/like/{userId}")
    public FilmResponse addLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        return filmInterface.addLike(userId, id);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public FilmResponse delLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        return filmInterface.delLike(userId, id);
    }

    @GetMapping("/popular")
    public LinkedHashSet<FilmResponse> viewRating(@RequestParam(defaultValue = "10") Long count) {
        return filmInterface.viewRating(count);
    }

    /**
     * преобразует FilmRequest в объект Buffer
     *
     * @param filmRequest объект FilmRequest
     * @return объект Buffer
     */
    private Buffer parseFilmRequestToBuffer(FilmResponse filmRequest) {
        List<String> genreIds = filmRequest.getGenres().stream()
                .map(Genre::getId) // Получаем id каждого жанра
                .map(String::valueOf) // Преобразуем Long в String
                .toList(); // Собираем в List

        return Buffer.of(
                filmRequest.getId(),
                filmRequest.getName(),
                filmRequest.getDescription(),
                filmRequest.getReleaseDate(),
                filmRequest.getDuration(),
                genreIds, // Теперь это List<String>
                filmRequest.getMpa().getId()
        );
    }

}
