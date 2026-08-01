package com.transport.authservice.service;

import com.transport.authservice.dto.response.AuthResponseDto;
import com.transport.authservice.entity.RefreshToken;
import com.transport.authservice.entity.User;
import com.transport.authservice.enums.Role;
import com.transport.authservice.repository.RefreshTokenRepository;
import com.transport.authservice.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTokenTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private JwtService jwtService;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                userRepository,
                refreshTokenRepository,
                otpService,
                jwtService,
                passwordEncoder);
    }

    @Test
    void refreshRotatesTokenAndQueriesOnlyItsHash() {
        String oldRawToken = "old-raw-refresh-token";
        String newRawToken = "new-raw-refresh-token";
        User user = activeUser();
        RefreshToken stored = RefreshToken.builder()
                .token("stored-hash")
                .userId(user.getUserId())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        Claims oldClaims = claims("REFRESH", "7");
        Claims newClaims = claims("REFRESH", "7");
        when(jwtService.extractClaims(oldRawToken)).thenReturn(oldClaims);
        when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(stored));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshtoken(user)).thenReturn(newRawToken);
        when(jwtService.extractClaims(newRawToken)).thenReturn(newClaims);

        AuthResponseDto response = service.refreshAccessToken(oldRawToken);

        ArgumentCaptor<String> lookup = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).findByTokenAndRevokedFalse(lookup.capture());
        assertThat(lookup.getValue())
                .isNotEqualTo(oldRawToken)
                .hasSize(43);
        verify(refreshTokenRepository).delete(stored);

        ArgumentCaptor<RefreshToken> replacement =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(replacement.capture());
        assertThat(replacement.getValue().getToken())
                .isNotEqualTo(newRawToken)
                .hasSize(43);
        assertThat(response.getRefreshToken()).isEqualTo(newRawToken);
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getUser().isActive()).isTrue();
        assertThat(response.getUser().getPreferredLanguage()).isEqualTo("en");
    }

    private User activeUser() {
        return User.builder()
                .userId(7L)
                .mobileNumber("9876543210")
                .name("Passenger")
                .role(Role.PASSENGER)
                .active(true)
                .preferredLanguage("en")
                .build();
    }

    private Claims claims(String type, String subject) {
        Claims claims = Jwts.claims().setSubject(subject);
        claims.put("type", type);
        claims.setExpiration(new Date(System.currentTimeMillis() + 60_000));
        return claims;
    }
}
