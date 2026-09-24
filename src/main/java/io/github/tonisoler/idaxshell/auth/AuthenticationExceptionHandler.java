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
