package com.technokratos.pact.auth.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueUserValidator.class)
@Documented
public @interface UniqueUser {
    String message() default "Пользователь с такими данными уже существует";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String[] fields() default {"email", "phoneNumber"};
}