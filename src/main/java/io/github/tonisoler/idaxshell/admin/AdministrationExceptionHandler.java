package io.github.tonisoler.idaxshell.admin;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {AdministrationController.class, TenantAdminController.class})
public class AdministrationExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> invalid(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message",
        exception.getMessage() == null ? "Invalid request" : exception.getMessage()));
  }

  /**
   * Answers a deliberate status (409 for deleting a template role, 404 for a missing workspace)
   * directly. Left to Spring's default it becomes an /error dispatch, which Spring Security denies:
   * the client saw an empty 403 instead of the status and its reason.
   */
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> status(ResponseStatusException exception) {
    String reason = exception.getReason() == null ? exception.getStatusCode().toString() : exception.getReason();
    return ResponseEntity.status(exception.getStatusCode()).body(Map.of("code", "STATUS_" + exception.getStatusCode().value(), "message", reason));
  }
}
