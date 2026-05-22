package com.technokratos.pact.common;

import com.technokratos.pact.article.exception.ArticleNotFoundException;
import com.technokratos.pact.chat.exception.ChatNotFoundException;
import com.technokratos.pact.chat.exception.MessageNotFoundException;
import com.technokratos.pact.file.exception.FileDeleteException;
import com.technokratos.pact.file.exception.FileNotFoundException;
import com.technokratos.pact.file.exception.FileStorageException;
import com.technokratos.pact.file.exception.FileUploadException;
import com.technokratos.pact.file.exception.FileValidationException;
import com.technokratos.pact.friendship.exception.FriendshipNotFoundException;
import com.technokratos.pact.game.exception.CannotLoadSteamSpyGamesException;
import com.technokratos.pact.security.exception.DisabledAccountException;
import com.technokratos.pact.user.exception.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ArticleNotFoundException.class,
            ChatNotFoundException.class,
            MessageNotFoundException.class,
            FriendshipNotFoundException.class,
            UserNotFoundException.class,
            FileNotFoundException.class
    })
    public String handleNotFound(RuntimeException ex, Model model) {
        log.warn("Not found: {}", ex.getMessage());
        model.addAttribute("error", "Не найдено");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("status", 404);
        return "error";
    }

    @ExceptionHandler(DisabledAccountException.class)
    public String handleDisabledAccount(DisabledAccountException ex, Model model) {
        log.warn("Disabled account: {}", ex.getMessage());
        model.addAttribute("error", "Аккаунт заблокирован");
        model.addAttribute("message", "Ваш аккаунт был заблокирован. Обратитесь к администратору.");
        model.addAttribute("status", 403);
        return "error";
    }

    @ExceptionHandler(FileValidationException.class)
    public String handleFileValidation(FileValidationException ex, Model model) {
        log.warn("File validation error: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка валидации файла");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("status", 400);
        return "error";
    }

    @ExceptionHandler(FileUploadException.class)
    public String handleFileUpload(FileUploadException ex, Model model) {
        log.error("File upload error: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка загрузки файла");
        model.addAttribute("message", "Не удалось загрузить файл. Попробуйте позже.");
        model.addAttribute("status", 500);
        return "error";
    }

    @ExceptionHandler(FileDeleteException.class)
    public String handleFileDelete(FileDeleteException ex, Model model) {
        log.error("File delete error: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка удаления файла");
        model.addAttribute("message", "Не удалось удалить файл.");
        model.addAttribute("status", 500);
        return "error";
    }

    @ExceptionHandler(FileStorageException.class)
    public String handleFileStorage(FileStorageException ex, Model model) {
        log.error("File storage error: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка хранилища");
        model.addAttribute("message", "Произошла ошибка при работе с файлами.");
        model.addAttribute("status", 500);
        return "error";
    }

    @ExceptionHandler(CannotLoadSteamSpyGamesException.class)
    public String handleSteamSpy(CannotLoadSteamSpyGamesException ex, Model model) {
        log.error("SteamSpy error: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка загрузки игр");
        model.addAttribute("message", "Не удалось загрузить список игр из Steam. Попробуйте позже.");
        model.addAttribute("status", 503);
        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidation(MethodArgumentNotValidException ex, Model model) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);
        model.addAttribute("error", "Ошибка валидации");
        model.addAttribute("message", errors);
        model.addAttribute("status", 400);
        return "error";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(ConstraintViolationException ex, Model model) {
        log.warn("Constraint violation: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка валидации");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("status", 400);
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex, Model model) {
        log.warn("Data integrity error: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка базы данных");
        model.addAttribute("message", "Нарушение целостности данных. Возможно, такой объект уже существует.");
        model.addAttribute("status", 409);
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex, Model model) {
        log.warn("Max upload size exceeded: {}", ex.getMessage());
        model.addAttribute("error", "Файл слишком большой");
        model.addAttribute("message", "Размер файла превышает допустимый лимит (максимум 10MB).");
        model.addAttribute("status", 400);
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Access denied: {}", ex.getMessage());
        model.addAttribute("error", "Доступ запрещён");
        model.addAttribute("message", "У вас нет прав для доступа к этой странице.");
        model.addAttribute("status", 403);
        return "error";
    }

    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthentication(AuthenticationException ex, Model model) {
        log.warn("Authentication error: {}", ex.getMessage());
        model.addAttribute("error", "Ошибка авторизации");
        model.addAttribute("message", "Пожалуйста, войдите в систему.");
        model.addAttribute("status", 401);
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResource(NoResourceFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("error", "Страница не найдена");
        model.addAttribute("message", "Запрашиваемая страница не существует.");
        model.addAttribute("status", 404);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        model.addAttribute("error", "Юзер дурак");
        model.addAttribute("message", "Научись пользоваться интернетом");
        model.addAttribute("status", 400);
        return "error";
    }
}