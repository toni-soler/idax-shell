package io.github.tonisoler.idaxshell.serviceauth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceAuthExceptionHandlerTest {
  ServiceAuthExceptionHandler handler = new ServiceAuthExceptionHandler();

  @Test void mapsTheUniformIssuerRejectionToUnauthorizedWithoutLeakingWhichCheckFailed() {
    var response = handler.invalidServiceCredentialsOrGrant();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    // Deliberately one fixed code/message for every rejection reason (unknown client, wrong
    // secret, wrong tenant, missing grant, over-wide permission request): a differentiated
    // response would let a caller enumerate which part of the request was wrong.
    assertThat(response.getBody()).containsEntry("code", "INVALID_SERVICE_CREDENTIALS_OR_GRANT");
  }
}
