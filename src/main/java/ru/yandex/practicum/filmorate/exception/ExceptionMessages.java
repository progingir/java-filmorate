package ru.yandex.practicum.filmorate.exception;

public class ExceptionMessages {

    //текс для ValidationException(фильмы)
    public static final String FILM_ID_CANNOT_BE_NULL = "Идентификатор фильма не может быть null";
    public static final String FILM_NOT_FOUND = "Фильм с id %d не найден";
    public static final String FILM_NAME_CANNOT_BE_EMPTY = "Название фильма не может быть пустым";
    public static final String FILM_DURATION_INVALID = "Продолжительность фильма должна быть положительным числом";
    public static final String FILM_DESCRIPTION_TOO_LONG = "Описание фильма не может превышать 200 символов";
    public static final String FILM_RELEASE_DATE_INVALID = "Дата релиза фильма не может быть раньше 28 декабря 1895 года";
}