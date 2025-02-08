package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(of = {"email"})
@AllArgsConstructor(staticName = "of")
public class User {
    private Long id;

    private String name;

    @Email(message = "Электронная почта должна быть корректной")
    @NotBlank(message = "Электронная почта не может быть пустой")
    private String email;

    @NotNull(message = "Логин не может быть null")
    @NotBlank(message = "Логин не может быть пустым и содержать пробелы")
    private String login;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Дата рождения не может быть null")
    private LocalDate birthday;
}