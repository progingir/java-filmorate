package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmResponse;
import ru.yandex.practicum.filmorate.service.FilmInterface;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("/films")
public class FilmController {

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
    public FilmResponse create(@Valid @RequestBody Film film) {
        return filmStorage.create(film);
    }

    @PutMapping
    public FilmResponse update(@Valid @RequestBody Film film) {
        return filmStorage.update(film);
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
}