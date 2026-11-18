package io.github.tonisoler.idaxshell.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A deliberate status reaches the client with its reason, instead of an /error dispatch (empty 403). */
class AdministrationExceptionHandlerTest {
  @Test void aDeliberateStatusIsAnsweredWithItsReason() {
    var response = new AdministrationExceptionHandler().status(
        new ResponseStatusException(HttpStatus.CONFLICT, "A module role template cannot be deleted; disable it instead"));
    assertEquals(409, response.getStatusCode().value());
    assertEquals("STATUS_409", response.getBody().get("code"));
    assertEquals("A module role template cannot be deleted; disable it instead", response.getBody().get("message"));
  }
}
