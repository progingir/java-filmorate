package ru.yandex.practicum.filmorate.exception;

public class ConditionsNotMetException extends RuntimeException {

    private final String id; // Идентификатор, связанный с ошибкой

    public ConditionsNotMetException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}