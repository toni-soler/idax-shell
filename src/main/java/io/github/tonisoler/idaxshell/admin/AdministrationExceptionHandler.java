package io.github.tonisoler.idaxshell.admin;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdministrationController.class)
public class AdministrationExceptionHandler {
  @ExceptionHandler(CoreCapabilityUnavailableException.class)
  ResponseEntity<Map<String, String>> unavailable(CoreCapabilityUnavailableException exception) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
        "code", "CORE_RUNTIME_UPGRADE_REQUIRED", "message", exception.getMessage()));
  }
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> invalid(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", exception.getMessage()));
  }
}
