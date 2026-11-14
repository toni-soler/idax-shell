package io.github.tonisoler.idaxshell.auth;

import es.idynamicsax.idax.service.security.AuthRateLimiterService;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthenticationController.class)
public class AuthenticationExceptionHandler {
  /**
   * Core's MFA services report outcomes as IllegalArgumentException codes (MfaLoginService,
   * MfaForcedSetupService). Without this they fell through to a bare 500, so a client could not
   * tell a wrong code from an expired challenge or a dead backend. Only these known, non-secret
   * codes are relayed; anything else stays an opaque failure. A wrong or reused code is 401; an
   * expired or unknown challenge/setup token means "start again" (also 401); a state conflict is 409.
   */
  static final Map<String, HttpStatus> MFA_OUTCOMES = Map.ofEntries(
      Map.entry("INVALID_TOTP", HttpStatus.UNAUTHORIZED),
      Map.entry("INVALID_RECOVERY_CODE", HttpStatus.UNAUTHORIZED),
      Map.entry("INVALID_MFA_CHALLENGE", HttpStatus.UNAUTHORIZED),
      Map.entry("INVALID_MFA_SETUP_TOKEN", HttpStatus.UNAUTHORIZED),
      Map.entry("MFA_SETUP_EXPIRED", HttpStatus.UNAUTHORIZED),
      Map.entry("MFA_SETUP_NOT_STARTED", HttpStatus.CONFLICT),
      Map.entry("MFA_ACTIVATION_CONFLICT", HttpStatus.CONFLICT),
      Map.entry("MFA_ALREADY_ENABLED", HttpStatus.CONFLICT),
      Map.entry("MFA_NOT_ENABLED", HttpStatus.CONFLICT),
      Map.entry("MFA_DISABLED_GLOBALLY", HttpStatus.CONFLICT));

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> mfaOutcome(IllegalArgumentException exception) {
    String code = exception.getMessage();
    HttpStatus status = code == null ? null : MFA_OUTCOMES.get(code);
    if (status == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "AUTHENTICATION_FAILED", "message", "Authentication failed"));
    }
    return ResponseEntity.status(status).body(Map.of("code", code, "message", code));
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Map<String, String>> invalidCredentials(AuthenticationException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("code", "INVALID_CREDENTIALS", "message", "Invalid credentials"));
  }

  // Previously uncaught (RateLimitedException extends RuntimeException, not AuthenticationException)
  // and fell through to a bare Spring 500 - a locked-out user saw the exact same generic failure
  // as a dead backend, with no indication that waiting would fix it.
  @ExceptionHandler(AuthRateLimiterService.RateLimitedException.class)
  ResponseEntity<Map<String, String>> rateLimited(AuthRateLimiterService.RateLimitedException exception) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("code", "RATE_LIMITED",
        "message", "Too many attempts", "lockedUntil", exception.lockedUntil.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
  }
}
