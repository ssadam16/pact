package com.technokratos.pact.auth.dto;

import com.technokratos.pact.auth.annotation.UniqueUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@UniqueUser
public record RegisterRequest(

        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "Имя пользователя может содержать только латинские буквы, цифры и символы . _ -")
        String username,

        @NotBlank(message = "Отображаемое имя не может быть пустым")
        @Size(min = 1, max = 50, message = "Отображаемое имя должно быть от 1 до 50 символов")
        @Pattern(regexp = "^[a-zA-Zа-яА-Я0-9\\s.-]+$",
                message = "Отображаемое имя может содержать буквы, цифры, пробелы и символы . -")
        String name,

        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        @Size(max = 100, message = "Email не может превышать 100 символов")
        String email,

        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
                message = "Пароль должен содержать хотя бы одну цифру, одну строчную и одну заглавную букву")
        String password

) {}