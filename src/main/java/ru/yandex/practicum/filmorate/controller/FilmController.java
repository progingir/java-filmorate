package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Buffer;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRequest;
import ru.yandex.practicum.filmorate.service.FilmInterface;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/films")

public class FilmController {
    @Autowired
    @Qualifier("FilmDbStorage")
    private final FilmStorage filmStorage;

    @Autowired
    @Qualifier("UserDbStorage")
    private final UserStorage userStorage;

    private final FilmInterface filmInterface;

    @Autowired
    public FilmController(FilmStorage filmStorage, UserStorage userStorage, FilmInterface filmInterface) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmInterface = filmInterface;
    }

    @GetMapping
    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    @GetMapping("/{id}")
    public FilmRequest findById(@PathVariable("id") Long id) throws ConditionsNotMetException, NotFoundException {
        return filmStorage.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmRequest create(@Valid @RequestBody ObjectNode objectNode) throws ConditionsNotMetException, NullPointerException {
        String name = objectNode.get("name").asText();
        String description = objectNode.get("description").asText();
        String releaseDate = objectNode.get("releaseDate").asText();
        Integer duration = objectNode.get("duration").asInt();
        List<String> mpa = objectNode.get("mpa").findValuesAsText("id");
        List<String> genres = new ArrayList<>();
        try {
            genres = objectNode.get("genres").findValuesAsText("id");
        } catch (NullPointerException e) {
            genres = List.of("нет жанра");
        } finally {
            return filmStorage.create(Buffer.of(Long.valueOf(0), name, description, LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")), duration, genres, Long.valueOf(mpa.get(0).toString())));
        }
    }

    @PutMapping
    public FilmRequest update(@Valid @RequestBody ObjectNode objectNode) throws ConditionsNotMetException, NotFoundException {
        Long id = objectNode.get("id").asLong();
        String name = objectNode.get("name").asText();
        String description = objectNode.get("description").asText();
        String releaseDate = objectNode.get("releaseDate").asText();
        Integer duration = objectNode.get("duration").asInt();
        List<String> mpa = objectNode.get("mpa").findValuesAsText("id");
        List<String> genres = new ArrayList<>();
        try {
            genres = objectNode.get("genres").findValuesAsText("id");
        } catch (NullPointerException e) {
            genres = List.of("нет жанра");
        } finally {
            return filmStorage.update(Buffer.of(id, name, description, LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")), duration, genres, Long.valueOf(mpa.get(0).toString())));
        }
    }

    @PutMapping("/{id}/like/{userId}")
    public FilmRequest addLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) throws ConditionsNotMetException {
        return filmInterface.addLike(userId, id);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public FilmRequest delLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) throws NotFoundException {
        return filmInterface.delLike(userId, id);
    }
}