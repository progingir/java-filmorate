package ru.yandex.practicum.filmorate.exception;

public class ExceptionMessages {

    public static final String FILM_ID_CANNOT_BE_NULL = "Film ID cannot be null";
    public static final String FILM_NOT_FOUND = "Film with id %d not found";
    public static final String FILM_NAME_CANNOT_BE_EMPTY = "Film name cannot be empty";
    public static final String FILM_DURATION_INVALID = "Film duration must be a positive number";
    public static final String FILM_DESCRIPTION_TOO_LONG = "Film description cannot exceed 200 characters";
    public static final String FILM_RELEASE_DATE_INVALID = "Film release date cannot be earlier than December 28, 1895";
}