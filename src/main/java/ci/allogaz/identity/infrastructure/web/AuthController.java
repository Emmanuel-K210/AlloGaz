package ci.allogaz.identity.infrastructure.web;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.identity.application.AuthTokens;
import ci.allogaz.identity.application.OtpAuthenticationService;
import ci.allogaz.identity.application.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Connexion par téléphone et code OTP SMS")
public class AuthController {

    private final OtpAuthenticationService otpAuthentication;
    private final TokenService tokens;

    public AuthController(OtpAuthenticationService otpAuthentication, TokenService tokens) {
        this.otpAuthentication = otpAuthentication;
        this.tokens = tokens;
    }

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envoie un code OTP par SMS (inscription ou connexion)")
    public OtpRequested requestOtp(@Valid @RequestBody OtpRequest request) {
        Duration ttl = otpAuthentication.requestCode(request.phone());
        return new OtpRequested(ttl.toSeconds());
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Vérifie le code OTP et renvoie les jetons ; crée le compte au premier passage")
    public TokenResponse verifyOtp(@Valid @RequestBody OtpVerification request) {
        return TokenResponse.from(otpAuthentication.verifyCode(request.phone(), request.code()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Échange un refresh token contre une nouvelle paire de jetons (rotation)")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(tokens.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Révoque le refresh token")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        tokens.logout(request.refreshToken());
    }

    public record OtpRequest(@NotBlank String phone) {
    }

    public record OtpRequested(long expiresInSeconds) {
    }

    public record OtpVerification(@NotBlank String phone,
                                  @NotBlank @Pattern(regexp = "\\d{4,8}", message = "code numérique attendu") String code) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
                                Instant refreshTokenExpiresAt, String tokenType, boolean newUser, UserResponse user) {

        static TokenResponse from(AuthTokens t) {
            return new TokenResponse(t.accessToken(), t.accessTokenExpiresAt(), t.refreshToken(),
                    t.refreshTokenExpiresAt(), "Bearer", t.newUser(), UserResponse.from(t.user()));
        }
    }
}
