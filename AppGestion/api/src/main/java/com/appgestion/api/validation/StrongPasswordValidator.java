package com.appgestion.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Valida {@link StrongPassword}. Los valores nulos se dejan a {@code @NotBlank}. */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String violation = PasswordPolicy.violation(value);
        if (violation == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(violation.replace("{", "").replace("}", ""))
                .addConstraintViolation();
        return false;
    }
}
