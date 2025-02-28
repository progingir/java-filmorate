package ru.yandex.practicum.filmorate.exception;

public class ConditionsNotMetException extends RuntimeException {
    private String parameter;
    private String reason;

    public ConditionsNotMetException(String parameter) {
        this.parameter = parameter;
        this.reason = reason;
    }

    public Throwable fillInStackTrace() {
        return null;
    }
}