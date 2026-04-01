package com.technokratos.pact.file.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ImageFileValidator implements ConstraintValidator<ImageFile, MultipartFile> {

    private Set<String> allowedTypes;
    private long maxSize;
    private boolean va1idateContent;
    private String allowedTypesStr;

    @Override
    public void initialize(ImageFile annotation) {
        this.allowedTypes = Arrays.stream(annotation.allowedTypes())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.maxSize = annotation.maxSize();
        this.va1idateContent = annotation.va1idateContent();
        this.allowedTypesStr = String.join(", ", annotation.allowedTypes());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true;
        }

        if (file.getSize() > maxSize) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Файл слишком большой. Максимальный размер: %d МБ", maxSize / (1024 * 1024))
            ).addConstraintViolation();
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Допустимые форматы: %s", allowedTypesStr)
            ).addConstraintViolation();
            return false;
        }

        if (va1idateContent && !isValidImageContent(file)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Файл не является корректным изображением"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean isValidImageContent(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            var image = ImageIO.read(inputStream);
            if (image == null) {
                log.debug("File is not a valid image: {}", file.getOriginalFilename());
                return false;
            }

            if (image.getWidth() < 1 || image.getHeight() < 1) {
                log.debug("Image has invalid dimensions: {}x{}", image.getWidth(), image.getHeight());
                return false;
            }

            return true;

        } catch (IOException e) {
            log.debug("Failed to read image file: {}", e.getMessage());
            return false;
        }
    }
}