package com.crm.commerce.platform.common.util;

import com.crm.commerce.platform.common.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void requireNonBlank_withValidValue_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireNonBlank("hello", "name").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireNonBlank_withBlank_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireNonBlank("", "name").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("name", "name must not be blank");
    }

    @Test
    void requireNonBlank_withNull_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireNonBlank(null, "field").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsKey("field");
    }

    @Test
    void requireNonNull_withValue_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireNonNull(new Object(), "obj").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireNonNull_withNull_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireNonNull(null, "obj").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("obj", "obj must not be null");
    }

    @Test
    void requireNonEmpty_withPopulatedCollection_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireNonEmpty(List.of("a"), "items").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireNonEmpty_withEmptyCollection_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireNonEmpty(Collections.emptyList(), "items").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("items", "items must not be empty");
    }

    @Test
    void requireNonEmpty_withPopulatedMap_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireNonEmpty(Map.of("k", "v"), "meta").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireNonEmpty_withEmptyMap_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireNonEmpty(Collections.emptyMap(), "meta").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("meta", "meta must not be empty");
    }

    @Test
    void requirePositive_withPositiveValue_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requirePositive(BigDecimal.TEN, "amount").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requirePositive_withZero_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requirePositive(BigDecimal.ZERO, "amount").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("amount", "amount must be positive");
    }

    @Test
    void requirePositive_withNull_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requirePositive(null, "amount").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsKey("amount");
    }

    @Test
    void requirePositiveInt_withPositiveValue_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requirePositiveInt(5, "qty").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requirePositiveInt_withZero_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requirePositiveInt(0, "qty").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("qty", "qty must be positive");
    }

    @Test
    void requireMinLength_withSufficientLength_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireMinLength("password", 6, "password").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireMinLength_withShortValue_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireMinLength("abc", 6, "password").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("password", "password must be at least 6 characters");
    }

    @Test
    void requireMaxLength_withinLimit_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireMaxLength("short", 10, "name").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireMaxLength_exceedsLimit_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireMaxLength("this is a very long name", 10, "name").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("name", "name must not exceed 10 characters");
    }

    @Test
    void requireValidEmail_withValidEmail_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireValidEmail("user@example.com", "email").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireValidEmail_withInvalidEmail_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireValidEmail("not-an-email", "email").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsEntry("email", "email must be a valid email address");
    }

    @Test
    void requireOneOf_withValidValue_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireOneOf("WEBSITE", "channel", "WEBSITE", "AMAZON", "BLINKIT").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireOneOf_caseInsensitive_passes() {
        assertThatCode(() ->
                ValidationUtils.validate().requireOneOf("website", "channel", "WEBSITE", "AMAZON", "BLINKIT").execute()
        ).doesNotThrowAnyException();
    }

    @Test
    void requireOneOf_withInvalidValue_throwsValidationException() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate().requireOneOf("FLIPKART", "channel", "WEBSITE", "AMAZON", "BLINKIT").execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).containsKey("channel");
        assertThat(ex.getErrors().get("channel")).contains("WEBSITE", "AMAZON", "BLINKIT");
    }

    @Test
    void multipleErrors_accumulatedInMap() {
        ValidationException ex = catchThrowableOfType(
                () -> ValidationUtils.validate()
                        .requireNonBlank("", "name")
                        .requireNonBlank("", "email")
                        .requirePositiveInt(0, "quantity")
                        .execute(),
                ValidationException.class);

        assertThat(ex.getErrors()).hasSize(3);
        assertThat(ex.getErrors()).containsKeys("name", "email", "quantity");
    }

    @Test
    void noErrors_doesNotThrow() {
        assertThatCode(() ->
                ValidationUtils.validate()
                        .requireNonBlank("value", "field1")
                        .requireNonNull("value", "field2")
                        .requirePositiveInt(1, "field3")
                        .execute()
        ).doesNotThrowAnyException();
    }
}
