package com.mahmoud.tazkara.event;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CategoryValidator implements ConstraintValidator<ValidCategory, String> {

    private static final Set<String> ALLOWED =
            Set.of("music", "conference", "culture", "film", "sport");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;   
        }
        return ALLOWED.contains(value.toLowerCase());
    }
}