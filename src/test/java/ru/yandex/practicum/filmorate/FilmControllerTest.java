package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.controller.FilmController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class FilmControllerTest {

    private static FilmController filmController;
    private static Film validFilm;
    private static Film invalidFilmName;
    private static Film invalidFilmDescription;
    private static Film invalidFilmReleaseDate;
    private static Film filmWithNoId;
    private static Film filmWithWrongId;

    @BeforeAll
    public static void start() throws ValidationException {
        filmController = new FilmController(new FilmService());

        validFilm = new Film();
        validFilm.setId(0L);
        validFilm.setName("Фильм");
        validFilm.setDescription("Описание");
        validFilm.setReleaseDate(LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        validFilm.setDuration(100);
        filmController.createFilm(validFilm);

        invalidFilmName = new Film();
        invalidFilmName.setId(0L);
        invalidFilmName.setName(" ");
        invalidFilmName.setDescription("Описание фильма без имени");
        invalidFilmName.setReleaseDate(LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        invalidFilmName.setDuration(100);

        invalidFilmDescription = new Film();
        invalidFilmDescription.setId(0L);
        invalidFilmDescription.setName("Название фильма");
        invalidFilmDescription.setDescription("Длинное длинное очень длинное описание " +
                "Чтобы проверить, как это все дело работает");
        invalidFilmDescription.setReleaseDate(LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        invalidFilmDescription.setDuration(100);

        invalidFilmReleaseDate = new Film();
        invalidFilmReleaseDate.setId(0L);
        invalidFilmReleaseDate.setName("Фильм с неправильной датой");
        invalidFilmReleaseDate.setDescription("Описание фильма с неправильной датой");
        invalidFilmReleaseDate.setReleaseDate(LocalDate.parse("1880-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        invalidFilmReleaseDate.setDuration(100);

        filmWithNoId = new Film();
        filmWithNoId.setId(null);
        filmWithNoId.setName("Фильм без айди");
        filmWithNoId.setDescription("Описание фильма без айди");
        filmWithNoId.setReleaseDate(LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        filmWithNoId.setDuration(100);

        filmWithWrongId = new Film();
        filmWithWrongId.setId(50L);
        filmWithWrongId.setName("Фильм с неправильным айди");
        filmWithWrongId.setDescription("Описание фильма с неправильным айди");
        filmWithWrongId.setReleaseDate(LocalDate.parse("2020-04-19", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        filmWithWrongId.setDuration(100);
    }

    @Test
    public void shouldCreateValidFilm() throws ValidationException {
        assertEquals(filmController.createFilm(validFilm), validFilm);
    }

    @Test
    public void shouldThrowExceptionWhenCreatingFilmWithEmptyName() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            filmController.createFilm(invalidFilmName);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowExceptionWhenCreatingFilmWithOldReleaseDate() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            filmController.createFilm(invalidFilmReleaseDate);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingFilmWithNoId() {
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            filmController.update(filmWithNoId);
        });
        assertNotNull(exception);
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingFilmWithWrongId() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            filmController.update(filmWithWrongId);
        });
        assertNotNull(exception);
    }

    @AfterAll
    public static void shouldReturnAllFilms() {
        assertNotNull(filmController.getFilms());
    }
}