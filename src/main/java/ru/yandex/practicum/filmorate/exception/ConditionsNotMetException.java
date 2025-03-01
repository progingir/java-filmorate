package ru.yandex.practicum.filmorate.exception;

import java.util.Objects;

public class ConditionsNotMetException extends RuntimeException {
    public ConditionsNotMetException(String message) {
        super(Objects.requireNonNull(message, "Сообщение об ошибке не может быть null"));
    }
}