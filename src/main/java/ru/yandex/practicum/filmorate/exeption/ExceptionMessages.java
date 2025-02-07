package ru.yandex.practicum.filmorate.exception;

public class ExceptionMessages {

    //текс для ValidationException(пользователь)
    public static final String USER_ID_CANNOT_BE_NULL = "Идентификатор пользователя не может быть нулевой";
    public static final String USER_NOT_FOUND = "Пользователь с данным идентификатором отсутствует в базе";
    public static final String EMAIL_CANNOT_BE_EMPTY = "Электронная почта не может быть пустой и должна содержать символ @";
    public static final String LOGIN_CANNOT_BE_EMPTY = "Логин не может быть пустым и содержать пробелы";
    public static final String BIRTHDAY_CANNOT_BE_IN_FUTURE = "Дата рождения не может быть в будущем";
    public static final String BIRTHDAY_CANNOT_BE_NULL = "Дата рождения не может быть нулевой";
    public static final String ID_CANNOT_BE_NULL = "Id должен быть указан";

    //текс для ValidationException(фильмы)
    public static final String FILM_ID_CANNOT_BE_NULL = "Идентификатор фильма не может быть null";
    public static final String FILM_NOT_FOUND = "Фильм с id %d не найден";
    public static final String FILM_NAME_CANNOT_BE_EMPTY = "Название фильма не может быть пустым";
    public static final String FILM_DURATION_INVALID = "Продолжительность фильма должна быть положительным числом";
    public static final String FILM_DESCRIPTION_TOO_LONG = "Описание фильма не может превышать 200 символов";
    public static final String FILM_RELEASE_DATE_INVALID = "Дата релиза фильма не может быть раньше 28 декабря 1895 года";

    //текс для DuplicatedDataException
    public static final String EMAIL_ALREADY_EXISTS = "Этот имейл уже используется";
}