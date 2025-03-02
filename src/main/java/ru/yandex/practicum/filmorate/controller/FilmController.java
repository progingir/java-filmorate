package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Buffer;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRequest;
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
            @Qualifier("FilmDbStorage") FilmStorage filmStorage,
            @Qualifier("UserDbStorage") UserStorage userStorage,
            FilmInterface filmInterface
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.filmInterface = filmInterface;
    }

    /**
     * Получить список всех фильмов.
     *
     * @return список всех фильмов
     */
    @GetMapping
    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    /**
     * Получить фильм по его идентификатору.
     *
     * @param id идентификатор фильма
     * @return объект фильма
     */
    @GetMapping("/{id}")
    public FilmRequest findById(@PathVariable("id") Long id) {
        return filmStorage.findById(id);
    }

    /**
     * Создать новый фильм.
     *
     * @param buffer объект запроса с данными фильма
     * @return созданный фильм
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FilmRequest create(@Valid @RequestBody Buffer buffer) {
        return filmStorage.create(buffer);
    }

    /**
     * Обновить данные фильма.
     *
     * @param buffer объект запроса с новыми данными фильма
     * @return обновленный фильм
     */
    @PutMapping
    public FilmRequest update(@Valid @RequestBody Buffer buffer) {
        return filmStorage.update(buffer);
    }

    /**
     * Добавить лайк фильму от пользователя.
     *
     * @param id     идентификатор фильма
     * @param userId идентификатор пользователя
     * @return фильм с обновленным списком лайков
     */
    @PutMapping("/{id}/like/{userId}")
    public FilmRequest addLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        return filmInterface.addLike(userId, id);
    }

    /**
     * Удалить лайк у фильма от пользователя.
     *
     * @param id     идентификатор фильма
     * @param userId идентификатор пользователя
     * @return фильм с обновленным списком лайков
     */
    @DeleteMapping("/{id}/like/{userId}")
    public FilmRequest delLike(@Valid @PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        return filmInterface.delLike(userId, id);
    }

    /**
     * Получить список самых популярных фильмов.
     *
     * @param count количество фильмов (необязательный параметр)
     * @return список фильмов, отсортированных по популярности
     */
    @GetMapping("/popular")
    public LinkedHashSet<FilmRequest> viewRating(@RequestParam(required = false) Long count) {
        return filmInterface.viewRating(count);
    }
}