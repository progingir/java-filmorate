package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor(staticName = "of")
public class Film {
    private Long id;

    @NotNull
    @NotBlank
    private String name;

    @Description("New film update description")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    @NotNull
    private Integer duration;

    @JsonIgnore
    private Set<Long> likedUsers;

    private Mpa mpa; // Изменено на объект Mpa
    private LinkedHashSet<Genre> genres; // Изменено на LinkedHashSet<Genre>

    // Если вам нужно, вы можете добавить методы для получения информации о MPA
    public Long getMpaId() {
        return mpa != null ? mpa.getId() : null;
    }
}

