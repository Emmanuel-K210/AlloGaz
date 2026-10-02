package ci.allogaz.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ci.allogaz.identity.application.port.out.OtpStore;
import ci.allogaz.identity.application.port.out.SmsSender;
import ci.allogaz.identity.application.port.out.UserRepository;
import ci.allogaz.identity.domain.PhoneNumber;
import ci.allogaz.shared.application.SecretHasher;
import ci.allogaz.shared.domain.TooManyRequestsException;
import ci.allogaz.shared.domain.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class OtpAuthenticationServiceTest {

    private static final PhoneNumber PHONE = PhoneNumber.parse("0701020304");

    @Mock OtpStore otpStore;
    @Mock SmsSender smsSender;
    @Mock UserRepository users;
    @Mock SecretHasher hasher;
    @Mock TokenService tokens;

    OtpAuthenticationService service;

    @BeforeEach
    void setUp() {
        OtpPolicy policy = new OtpPolicy(6, Duration.ofMinutes(5), 3, Duration.ofSeconds(60), 5);
        service = new OtpAuthenticationService(otpStore, smsSender, users, hasher, tokens, policy,
                Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void sends_a_hashed_code_by_sms() {
        when(otpStore.resendCooldown(PHONE)).thenReturn(Optional.empty());
        when(otpStore.incrementSendCount(eq(PHONE), any())).thenReturn(1L);
        when(hasher.hash(anyString())).thenReturn("hash");

        assertThat(service.requestCode("07 01 02 03 04")).isEqualTo(Duration.ofMinutes(5));

        verify(otpStore).storeCode(PHONE, "hash", Duration.ofMinutes(5), Duration.ofSeconds(60));
        verify(smsSender).send(eq(PHONE), anyString());
    }

    @Test
    void refuses_a_new_code_during_cooldown() {
        when(otpStore.resendCooldown(PHONE)).thenReturn(Optional.of(Duration.ofSeconds(42)));

        assertThatThrownBy(() -> service.requestCode("0701020304"))
                .isInstanceOf(TooManyRequestsException.class).hasMessageContaining("42 s");
        verify(smsSender, never()).send(any(), anyString());
    }

    @Test
    void refuses_beyond_hourly_send_limit() {
        when(otpStore.resendCooldown(PHONE)).thenReturn(Optional.empty());
        when(otpStore.incrementSendCount(eq(PHONE), any())).thenReturn(6L);

        assertThatThrownBy(() -> service.requestCode("0701020304")).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void wrong_code_reports_remaining_attempts() {
        when(otpStore.findCodeHash(PHONE)).thenReturn(Optional.of("hash"));
        when(otpStore.incrementAttempts(PHONE)).thenReturn(1L);
        when(hasher.matches(anyString(), eq("hash"))).thenReturn(false);

        assertThatThrownBy(() -> service.verifyCode("0701020304", "000000"))
                .isInstanceOf(UnauthorizedException.class).hasMessageContaining("Essais restants : 2");
        verify(otpStore, never()).deleteCode(PHONE);
    }

    @Test
    void last_wrong_attempt_burns_the_code() {
        when(otpStore.findCodeHash(PHONE)).thenReturn(Optional.of("hash"));
        when(otpStore.incrementAttempts(PHONE)).thenReturn(3L);
        when(hasher.matches(anyString(), eq("hash"))).thenReturn(false);

        assertThatThrownBy(() -> service.verifyCode("0701020304", "000000"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(otpStore).deleteCode(PHONE);
    }

    @Test
    void expired_code_is_rejected() {
        when(otpStore.findCodeHash(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCode("0701020304", "123456"))
                .isInstanceOf(UnauthorizedException.class).hasMessageContaining("expiré");
    }
}
