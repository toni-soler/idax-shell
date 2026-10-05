package io.github.tonisoler.idaxshell.serviceauth;

import es.idynamicsax.idax.service.auth.ServiceAuthenticationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ServiceTokenIssuer deliberately collapses every failure mode - unknown client id, wrong
 * secret, inactive/non-existent tenant, missing or expired grant, a requested permission the
 * grant does not cover - into the same ServiceAuthenticationException, so this handler cannot
 * and must not add detail: a differentiated response here would reopen exactly the
 * client/tenant/grant enumeration the issuer's single exception type exists to prevent.
 */
@RestControllerAdvice(assignableTypes = ServiceAuthController.class)
public class ServiceAuthExceptionHandler {
  @ExceptionHandler(ServiceAuthenticationException.class)
  ResponseEntity<Map<String, String>> invalidServiceCredentialsOrGrant() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("code", "INVALID_SERVICE_CREDENTIALS_OR_GRANT",
            "message", "Invalid service credentials or grant"));
  }
}
