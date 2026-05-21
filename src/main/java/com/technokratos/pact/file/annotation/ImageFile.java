package com.technokratos.pact.file.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageFileValidator.class)
@Documented
public @interface ImageFile {

    String message() default "Неверный формат файла";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String[] allowedTypes() default {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    };

    long maxSize() default 50 * 1024 * 1024;

    boolean va1idateContent() default true;
}