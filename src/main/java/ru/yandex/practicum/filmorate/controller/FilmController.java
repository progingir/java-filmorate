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
    public FilmRequest create(@RequestBody ObjectNode objectNode) throws ConditionsNotMetException, NullPointerException {
        // Проверка названия фильма
        String name = objectNode.get("name").asText();
        if (name == null || name.isBlank()) {
            throw new ConditionsNotMetException("Название не может быть пустым");
        }

        // Проверка описания фильма
        String description = objectNode.get("description").asText();
        if (description != null && description.length() > 200) {
            throw new ConditionsNotMetException("Максимальная длина описания — 200 символов");
        }

        // Проверка даты выпуска
        String releaseDateStr = objectNode.get("releaseDate").asText();
        LocalDate releaseDate;
        try {
            releaseDate = LocalDate.parse(releaseDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new ConditionsNotMetException("Неверный формат даты выпуска. Используйте формат 'yyyy-MM-dd'");
        }
        if (releaseDate.isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года");
        }

        // Проверка продолжительности
        Integer duration = objectNode.get("duration").asInt();
        if (duration == null || duration <= 0) {
            throw new ConditionsNotMetException("Продолжительность должна быть положительным числом");
        }

        // Проверка MPA
        List<String> mpa = objectNode.get("mpa").findValuesAsText("id");
        if (mpa == null || mpa.isEmpty()) {
            throw new ConditionsNotMetException("Рейтинг MPA должен быть указан");
        }
        Long mpaId = Long.valueOf(mpa.get(0).toString());
        if (mpaId < 1 || mpaId > 5) {
            throw new ConditionsNotMetException("Некорректный рейтинг MPA");
        }

        // Проверка жанров
        List<String> genres = new ArrayList<>();
        try {
            genres = objectNode.get("genres").findValuesAsText("id");
        } catch (NullPointerException e) {
            genres = List.of("нет жанра");
        }
        for (String genre : genres) {
            try {
                Long genreId = Long.parseLong(genre);
                if (genreId < 1 || genreId > 6) {
                    throw new ConditionsNotMetException("Некорректный жанр: " + genreId);
                }
            } catch (NumberFormatException ex) {
                throw new ConditionsNotMetException("Неверный формат жанра: " + genre);
            }
        }

        // Создание фильма
        return filmStorage.create(Buffer.of(
                Long.valueOf(0),
                name,
                description,
                releaseDate,
                duration,
                genres,
                mpaId
        ));
    }

    @PutMapping
    public FilmRequest update(@RequestBody ObjectNode objectNode) throws ConditionsNotMetException, NotFoundException {
        // Проверка ID фильма
        Long id = objectNode.get("id").asLong();
        if (id == null || id <= 0) {
            throw new ConditionsNotMetException("ID фильма должен быть положительным числом");
        }

        // Проверка названия фильма
        String name = objectNode.get("name").asText();
        if (name == null || name.isBlank()) {
            throw new ConditionsNotMetException("Название не может быть пустым");
        }

        // Проверка описания фильма
        String description = objectNode.get("description").asText();
        if (description != null && description.length() > 200) {
            throw new ConditionsNotMetException("Максимальная длина описания — 200 символов");
        }

        // Проверка даты выпуска
        String releaseDateStr = objectNode.get("releaseDate").asText();
        LocalDate releaseDate;
        try {
            releaseDate = LocalDate.parse(releaseDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new ConditionsNotMetException("Неверный формат даты выпуска. Используйте формат 'yyyy-MM-dd'");
        }
        if (releaseDate.isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ConditionsNotMetException("Дата релиза — не раньше 28 декабря 1895 года");
        }

        // Проверка продолжительности
        Integer duration = objectNode.get("duration").asInt();
        if (duration == null || duration <= 0) {
            throw new ConditionsNotMetException("Продолжительность должна быть положительным числом");
        }

        // Проверка MPA
        List<String> mpa = objectNode.get("mpa").findValuesAsText("id");
        if (mpa == null || mpa.isEmpty()) {
            throw new ConditionsNotMetException("Рейтинг MPA должен быть указан");
        }
        Long mpaId = Long.valueOf(mpa.get(0).toString());
        if (mpaId < 1 || mpaId > 5) {
            throw new ConditionsNotMetException("Некорректный рейтинг MPA");
        }

        // Проверка жанров
        List<String> genres = new ArrayList<>();
        try {
            genres = objectNode.get("genres").findValuesAsText("id");
        } catch (NullPointerException e) {
            genres = List.of("нет жанра");
        }
        for (String genre : genres) {
            try {
                Long genreId = Long.parseLong(genre);
                if (genreId < 1 || genreId > 6) {
                    throw new ConditionsNotMetException("Некорректный жанр: " + genreId);
                }
            } catch (NumberFormatException ex) {
                throw new ConditionsNotMetException("Неверный формат жанра: " + genre);
            }
        }

        // Обновление фильма
        return filmStorage.update(Buffer.of(
                id,
                name,
                description,
                releaseDate,
                duration,
                genres,
                mpaId
        ));
    }

    @PutMapping("/{id}/like/{userId}")
    public FilmRequest addLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) throws ConditionsNotMetException {
        return filmInterface.addLike(userId, id);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public FilmRequest delLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) throws NotFoundException {
        return filmInterface.delLike(userId, id);
    }

    @GetMapping("/popular")
    public LinkedHashSet<FilmRequest> viewRaiting(@RequestParam(required = false) Long count) throws NotFoundException {
        return filmInterface.viewRating(count);
    }
}