package com.technokratos.pact.auth.annotation;

import com.technokratos.pact.user.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class UniqueUserValidator implements ConstraintValidator<UniqueUser, Object> {

    private final UserRepository userRepository;
    private String[] fields;

    @Override
    public void initialize(UniqueUser constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean isValid = true;

        for (String fieldName : fields) {
            try {
                Object value = getFieldValue(request, fieldName);
                if (value == null) continue;

                boolean exists = checkExists(fieldName, value);

                if (exists) {
                    addConstraintViolation(context, fieldName,
                            "%s '%s' уже используется".formatted(fieldName, value));

                    isValid = false;
                }

            } catch (Exception e) {
                log.error("Error while unique validation: {}", e.getMessage());
                throw new RuntimeException("Error while unique validation");
            }
        }

        return isValid;
    }

    private Object getFieldValue(Object request, String fieldName) throws Exception {
        if (request.getClass().isRecord()) {
            var component = Arrays.stream(request.getClass().getRecordComponents())
                    .filter(c -> c.getName().equals(fieldName))
                    .findFirst()
                    .orElse(null);
            if (component != null) {
                return component.getAccessor().invoke(request);
            }
        } else {
            Field field = request.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(request);
        }
        return null;
    }

    private boolean checkExists(String fieldName, Object value) {
        return switch (fieldName) {
            case "email" -> userRepository.existsByEmail((String) value);
            case "username" -> userRepository.existsByUsername((String) value);
            default -> false;
        };
    }

    private void addConstraintViolation(ConstraintValidatorContext context,
                                        String field, String message) {

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}