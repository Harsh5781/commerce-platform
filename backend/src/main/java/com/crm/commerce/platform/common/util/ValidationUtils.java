package com.crm.commerce.platform.common.util;

import com.crm.commerce.platform.common.exception.ValidationException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationUtils {

    private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    private ValidationUtils() {
    }

    public static Validator validate() {
        return new Validator();
    }

    public static class Validator {

        private final Map<String, String> errors = new LinkedHashMap<>();

        public Validator requireNonBlank(String value, String fieldName) {
            if (!StringUtils.hasText(value)) {
                errors.put(fieldName, fieldName + " must not be blank");
            }
            return this;
        }

        public Validator requireNonNull(Object value, String fieldName) {
            if (value == null) {
                errors.put(fieldName, fieldName + " must not be null");
            }
            return this;
        }

        public Validator requireNonEmpty(Collection<?> collection, String fieldName) {
            if (CollectionUtils.isEmpty(collection)) {
                errors.put(fieldName, fieldName + " must not be empty");
            }
            return this;
        }

        public Validator requireNonEmpty(Map<?, ?> map, String fieldName) {
            if (CollectionUtils.isEmpty(map)) {
                errors.put(fieldName, fieldName + " must not be empty");
            }
            return this;
        }

        public Validator requirePositive(BigDecimal value, String fieldName) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                errors.put(fieldName, fieldName + " must be positive");
            }
            return this;
        }

        public Validator requirePositiveInt(int value, String fieldName) {
            if (value <= 0) {
                errors.put(fieldName, fieldName + " must be positive");
            }
            return this;
        }

        public Validator requireMinLength(String value, int min, String fieldName) {
            if (StringUtils.hasText(value) && value.length() < min) {
                errors.put(fieldName, fieldName + " must be at least " + min + " characters");
            }
            return this;
        }

        public Validator requireMaxLength(String value, int max, String fieldName) {
            if (StringUtils.hasText(value) && value.length() > max) {
                errors.put(fieldName, fieldName + " must not exceed " + max + " characters");
            }
            return this;
        }

        public Validator requireValidEmail(String email, String fieldName) {
            if (StringUtils.hasText(email) && !email.matches(EMAIL_REGEX)) {
                errors.put(fieldName, fieldName + " must be a valid email address");
            }
            return this;
        }

        public Validator requireOneOf(String value, String fieldName, String... allowed) {
            if (StringUtils.hasText(value)) {
                boolean found = false;
                for (String a : allowed) {
                    if (a.equalsIgnoreCase(value)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    errors.put(fieldName, fieldName + " must be one of: " + String.join(", ", allowed));
                }
            }
            return this;
        }

        public void execute() {
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
        }
    }
}
