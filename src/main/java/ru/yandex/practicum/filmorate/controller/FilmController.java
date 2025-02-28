package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRequest;
import ru.yandex.practicum.filmorate.service.FilmInterface;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/films")

public class FilmController {

    private final FilmStorage filmStorage;
    private final FilmInterface filmInterface;
    private final UserStorage userStorage;

    @Autowired
    public FilmController(FilmStorage filmStorage, UserStorage userStorage, FilmInterface filmInterface) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmInterface = filmInterface;
    }

    @GetMapping
    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable("id") Long id) throws ConditionsNotMetException, NotFoundException {
        return filmStorage.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film create(@Valid @RequestBody Film film) throws ConditionsNotMetException, NullPointerException {
        return filmStorage.create(film);
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film newFilm) throws ConditionsNotMetException, NotFoundException {
        return filmStorage.update(newFilm);
    }

    @PutMapping("/{id}/like/{userId}")
    public FilmRequest addLike(@Valid @RequestBody @PathVariable("id") Long id, @PathVariable("userId") Long userId) throws ConditionsNotMetException {
        return filmInterface.addLike(userId, id);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public FilmRequest delLike(@Valid @RequestBody @PathVariable("id") Long id, @PathVariable("userId") Long userId) throws NotFoundException {
        return filmInterface.delLike(userId, id);
    }

    @GetMapping("/popular")
    public List<Film> viewRaiting(@RequestParam(required = false) Long count) throws NotFoundException {
        return filmInterface.viewRaiting(count);
    }
}