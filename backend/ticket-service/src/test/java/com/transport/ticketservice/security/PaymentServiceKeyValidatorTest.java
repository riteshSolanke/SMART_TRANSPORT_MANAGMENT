package com.transport.ticketservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceKeyValidatorTest {
    private static final String SECRET =
            "payment-test-secret-that-is-at-least-32-characters";

    @Test
    void acceptsMatchingInternalSecret() {
        PaymentServiceKeyValidator validator =
                new PaymentServiceKeyValidator(SECRET);

        assertThatCode(() -> validator.validate(SECRET))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrIncorrectInternalSecret() {
        PaymentServiceKeyValidator validator =
                new PaymentServiceKeyValidator(SECRET);

        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> validator.validate("wrong-secret"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsWeakConfiguredSecret() {
        assertThatThrownBy(() ->
                new PaymentServiceKeyValidator("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
