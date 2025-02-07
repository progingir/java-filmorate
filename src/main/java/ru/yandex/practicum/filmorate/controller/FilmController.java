package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;

@RestController
@RequestMapping("/films")
public class FilmController {

    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    //создание фильма
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film createFilm(@Valid @RequestBody Film film) throws NullPointerException, ValidationException {
        return filmService.createFilm(film);
    }

    //получение фильма по айдишнику
    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable("id") String id) throws ValidationException {
        return filmService.getFilmById(Long.valueOf(id));
    }

    //обновление фильма
    @PutMapping
    public Film update(@Valid @RequestBody Film newFilm) throws ValidationException, NotFoundException {
        return filmService.update(newFilm);
    }

    //получение всех фильмов
    @GetMapping
    public Collection<Film> getFilms() {
        return filmService.getFilms();
    }
}