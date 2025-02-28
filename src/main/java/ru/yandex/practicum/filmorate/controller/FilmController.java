package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/films")
public class FilmController {

    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    public List<Film> findAll() {
        return new ArrayList<>(filmService.getFilms());
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable("id") Long id) throws NotFoundException {
        return filmService.getFilmById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film create(@Valid @RequestBody ObjectNode objectNode) throws NotFoundException {
        Film film = parseFilmFromObjectNode(objectNode);
        return filmService.createFilm(film);
    }

    @PutMapping
    public Film update(@Valid @RequestBody ObjectNode objectNode) throws NotFoundException {
        Film film = parseFilmFromObjectNode(objectNode);
        return filmService.update(film);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable("id") Long id, @PathVariable("userId") Long userId) throws NotFoundException {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void delLike(@PathVariable("id") Long id, @PathVariable("userId") Long userId) throws NotFoundException {
        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> viewRaiting(@RequestParam(required = false, defaultValue = "10") int count) {
        return filmService.getTopFilms(count);
    }

    private Film parseFilmFromObjectNode(ObjectNode objectNode) {
        Long id = objectNode.has("id") ? objectNode.get("id").asLong() : null;
        String name = objectNode.get("name").asText();
        String description = objectNode.get("description").asText();
        LocalDate releaseDate = LocalDate.parse(objectNode.get("releaseDate").asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Integer duration = objectNode.get("duration").asInt();
        Set<Long> likedUsers = new HashSet<>();
        Long mpaId = objectNode.get("mpa").get("id").asLong();
        LinkedHashSet<Long> genres = new LinkedHashSet<>(); // Используем LinkedHashSet
        objectNode.get("genres").forEach(genre -> genres.add(genre.get("id").asLong()));

        return Film.builder()
                .id(id)
                .name(name)
                .description(description)
                .releaseDate(releaseDate)
                .duration(duration)
                .likedUsers(likedUsers)
                .mpaId(mpaId)
                .genres(genres)
                .build();
    }
}