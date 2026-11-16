package io.github.tonisoler.idaxshell.auth;

import es.idynamicsax.idax.service.security.AuthRateLimiterService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Before this handler existed, every credential-related failure (wrong password, disabled
 * account, unknown email - idax-core's LocalCredentialAuthenticator deliberately collapses all
 * three into one IllegalArgumentException("INVALID_CREDENTIALS"), by design) surfaced as a bare
 * 401 with no machine-readable code, and AuthRateLimiterService.RateLimitedException - a
 * genuinely distinct, safe-to-reveal case - fell through uncaught to an unhandled Spring 500.
 * Both looked identical to the frontend, which is what produced "servicio desconectado" for a
 * simple bad password. See AJUSTES_PARA_CLAUDE_CODE.md's P1 "login atribuye todos los fallos".
 */
class AuthenticationExceptionHandlerTest {
    private final AuthenticationExceptionHandler handler = new AuthenticationExceptionHandler();

    @Test void invalidCredentialsMapsTo401WithAStableCode() {
        var response = handler.invalidCredentials(new BadCredentialsException("Invalid credentials"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", response.getBody().get("code"));
    }

    @Test void rateLimitedMapsTo429WithTheLockoutDeadline() {
        var lockedUntil = OffsetDateTime.now().plusMinutes(5);
        var response = handler.rateLimited(new AuthRateLimiterService.RateLimitedException(lockedUntil));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("RATE_LIMITED", response.getBody().get("code"));
        assertEquals(lockedUntil.toString(), OffsetDateTime.parse(response.getBody().get("lockedUntil")).toString());
    }

    @Test void knownMfaOutcomesKeepTheirCodeAndAStatusAClientCanActOn() {
        var wrong = handler.mfaOutcome(new IllegalArgumentException("INVALID_TOTP"));
        assertEquals(HttpStatus.UNAUTHORIZED, wrong.getStatusCode());
        assertEquals("INVALID_TOTP", wrong.getBody().get("code"));
        assertEquals(HttpStatus.UNAUTHORIZED, handler.mfaOutcome(new IllegalArgumentException("INVALID_MFA_CHALLENGE")).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, handler.mfaOutcome(new IllegalArgumentException("MFA_SETUP_EXPIRED")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, handler.mfaOutcome(new IllegalArgumentException("MFA_ACTIVATION_CONFLICT")).getStatusCode());
    }

    @Test void anyOtherArgumentFailureStaysOpaque() {
        var other = handler.mfaOutcome(new IllegalArgumentException("Some internal detail about user 42"));
        assertEquals(HttpStatus.BAD_REQUEST, other.getStatusCode());
        assertEquals("AUTHENTICATION_FAILED", other.getBody().get("code"));
        assertFalse(other.getBody().values().stream().anyMatch(v -> v.contains("42")));
        assertEquals("AUTHENTICATION_FAILED", handler.mfaOutcome(new IllegalArgumentException((String) null)).getBody().get("code"));
    }
}
