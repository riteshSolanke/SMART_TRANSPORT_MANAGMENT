package com.transport.authservice.service;

import com.transport.authservice.entity.User;
import com.transport.authservice.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "test-jwt-secret-that-is-at-least-thirty-two-bytes";

    private final JwtService jwtService =
            new JwtService(SECRET, 900_000, 604_800_000);

    @Test
    void refreshTokensAreUniqueEvenWhenGeneratedImmediately() {
        User user = User.builder()
                .userId(7L)
                .mobileNumber("9876543210")
                .role(Role.PASSENGER)
                .preferredLanguage("en")
                .build();

        String first = jwtService.generateRefreshtoken(user);
        String second = jwtService.generateRefreshtoken(user);

        assertThat(second).isNotEqualTo(first);
        Claims firstClaims = jwtService.extractClaims(first);
        Claims secondClaims = jwtService.extractClaims(second);
        assertThat(firstClaims.getId()).isNotBlank();
        assertThat(secondClaims.getId()).isNotEqualTo(firstClaims.getId());
        assertThat(secondClaims.get("type", String.class)).isEqualTo("REFRESH");
    }
}
