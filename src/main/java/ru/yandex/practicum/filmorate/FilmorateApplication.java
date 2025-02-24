package ru.yandex.practicum.filmorate;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;

//@SpringBootApplication
@ComponentScan(basePackages = {"ru.yandex.practicum.filmorate.exception", "ru.yandex.practicum.filmorate.controller"})
public class FilmorateApplication {
    public static void main(String[] args) {
        SpringApplication.run(FilmorateApplication.class, args);
    }
}