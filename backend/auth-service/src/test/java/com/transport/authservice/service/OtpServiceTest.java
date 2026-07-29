package com.transport.authservice.service;

import com.transport.authservice.entity.OtpVerification;
import com.transport.authservice.enums.OtpPurpose;
import com.transport.authservice.exception.InvalidTokenException;
import com.transport.authservice.exception.OtpAttemptExceededException;
import com.transport.authservice.exception.OtpRateLimitException;
import com.transport.authservice.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository repository;
    @Mock
    private OtpSender otpSender;
    @Mock
    private EmailSender emailSender;

    private BCryptPasswordEncoder encoder;
    private OtpService service;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder(4);
        service = new OtpService(
                repository, otpSender, emailSender, encoder, 5, 3, 60);
    }

    @Test
    void rejectsAndConsumesExpiredOtp() {
        OtpVerification record = record("123456", 0,
                LocalDateTime.now().minusSeconds(1));
        when(repository.findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.LOGIN)).thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
                service.verifyOtp("9876543210", "123456", OtpPurpose.LOGIN))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("otp.expired");

        assertThat(record.isUsed()).isTrue();
        verify(repository).save(record);
    }

    @Test
    void locksOtpOnConfiguredFinalAttempt() {
        OtpVerification record = record("123456", 2,
                LocalDateTime.now().plusMinutes(5));
        when(repository.findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.LOGIN)).thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
                service.verifyOtp("9876543210", "000000", OtpPurpose.LOGIN))
                .isInstanceOf(OtpAttemptExceededException.class)
                .hasMessage("otp.max.attempts");

        assertThat(record.getAttemptCount()).isEqualTo(3);
        verify(repository).save(record);
    }

    @Test
    void enforcesResendCooldown() {
        OtpVerification recent = record("123456", 0,
                LocalDateTime.now().plusMinutes(5));
        recent.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        when(repository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.REGISTRATION))
                .thenReturn(Optional.of(recent));

        assertThatThrownBy(() ->
                service.generateAndSendOtp("9876543210", OtpPurpose.REGISTRATION))
                .isInstanceOf(OtpRateLimitException.class)
                .hasMessage("otp.resend.too.soon");

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(otpSender, never()).send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void validEmailOtpIsMarkedUsed() {
        OtpVerification record = record("123456", 0,
                LocalDateTime.now().plusMinutes(5));
        when(repository.findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                "p@example.com", OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(record));

        service.verifyOtp("p@example.com", "123456", OtpPurpose.EMAIL_VERIFICATION);

        assertThat(record.isUsed()).isTrue();
        verify(repository).save(record);
    }

    private OtpVerification record(String rawOtp, int attempts, LocalDateTime expiresAt) {
        return OtpVerification.builder()
                .identifier("9876543210")
                .purpose(OtpPurpose.LOGIN)
                .otpCode(encoder.encode(rawOtp))
                .attemptCount(attempts)
                .expiresAt(expiresAt)
                .build();
    }
}
