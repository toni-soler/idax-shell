package io.github.tonisoler.idaxshell.serviceauth;

import es.idynamicsax.idax.service.auth.ServiceAuthenticationException;
import es.idynamicsax.idax.service.auth.ServiceTokenIssuer;
import es.idynamicsax.idax.service.auth.ServiceTokenRequest;
import es.idynamicsax.idax.service.auth.ServiceTokenResponse;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The controller is a thin wrapper: every credential/tenant/audience/grant decision belongs to
 * ServiceTokenIssuer (idax-core), already covered there (and by idax-app's
 * ServicePrincipalAuthenticationPostgresTest against a real database) for wrong client id, wrong
 * secret, wrong tenant, missing/insufficient grant and the happy path. These tests only prove
 * this controller passes the request through unchanged and does not swallow or translate the
 * issuer's exception itself (ServiceAuthExceptionHandlerTest covers the HTTP mapping).
 */
class ServiceAuthControllerTest {
  ServiceTokenIssuer issuer = mock(ServiceTokenIssuer.class);
  ServiceAuthController controller = new ServiceAuthController(issuer);

  @Test void issuesTheTokenTheIssuerReturnsForAFullyGrantedRequest() {
    UUID tenantId = UUID.randomUUID();
    var request = new ServiceTokenRequest("ostris-ledger-delivery", "correct-secret", tenantId,
        "idax-ledger", Set.of("LEDGER_PROOF_CREATE", "LEDGER_READ", "LEDGER_PROOF_VERIFY"));
    var expected = new ServiceTokenResponse("signed-jwt", "Bearer", Instant.now().plusSeconds(300), 300);
    when(issuer.issue(request)).thenReturn(expected);

    var response = controller.token(request);

    assertThat(response).isSameAs(expected);
    verify(issuer).issue(request);
  }

  @Test void noCredentialsPropagatesTheIssuersRejection() {
    var blank = new ServiceTokenRequest(null, null, null, null, null);
    when(issuer.issue(blank)).thenThrow(new ServiceAuthenticationException());
    assertThatThrownBy(() -> controller.token(blank)).isInstanceOf(ServiceAuthenticationException.class);
  }

  @Test void wrongClientIdPropagatesTheIssuersRejection() {
    var request = new ServiceTokenRequest("no-such-principal", "secret", UUID.randomUUID(), "idax-ledger", Set.of());
    when(issuer.issue(any())).thenThrow(new ServiceAuthenticationException());
    assertThatThrownBy(() -> controller.token(request)).isInstanceOf(ServiceAuthenticationException.class);
  }

  @Test void wrongSecretPropagatesTheIssuersRejection() {
    var request = new ServiceTokenRequest("ostris-ledger-delivery", "wrong-secret", UUID.randomUUID(), "idax-ledger", Set.of());
    when(issuer.issue(any())).thenThrow(new ServiceAuthenticationException());
    assertThatThrownBy(() -> controller.token(request)).isInstanceOf(ServiceAuthenticationException.class);
  }

  @Test void wrongTenantPropagatesTheIssuersRejection() {
    var request = new ServiceTokenRequest("ostris-ledger-delivery", "correct-secret", UUID.randomUUID(), "idax-ledger", Set.of());
    when(issuer.issue(any())).thenThrow(new ServiceAuthenticationException());
    assertThatThrownBy(() -> controller.token(request)).isInstanceOf(ServiceAuthenticationException.class);
  }

  @Test void missingGrantPropagatesTheIssuersRejection() {
    var request = new ServiceTokenRequest("ostris-ledger-delivery", "correct-secret", UUID.randomUUID(), "an-unrelated-audience", Set.of());
    when(issuer.issue(any())).thenThrow(new ServiceAuthenticationException());
    assertThatThrownBy(() -> controller.token(request)).isInstanceOf(ServiceAuthenticationException.class);
  }

  @Test void requestingAPermissionOutsideTheGrantPropagatesTheIssuersRejection() {
    // The issuer itself enforces that emitted permissions/audience never exceed the grant
    // (ServiceTokenIssuer.issue: "if(!granted.containsAll(emitted)) throw ...", and the JWT's
    // own "aud" claim is the single requested audience) - a token literally cannot carry a wider
    // audience/permission set than what was granted. This test only proves the controller does
    // not widen or strip that request before handing it to the issuer.
    var request = new ServiceTokenRequest("ostris-ledger-delivery", "correct-secret", UUID.randomUUID(),
        "idax-ledger", Set.of("LEDGER_PROOF_CREATE", "SOME_PERMISSION_NEVER_GRANTED"));
    when(issuer.issue(request)).thenThrow(new ServiceAuthenticationException());

    assertThatThrownBy(() -> controller.token(request)).isInstanceOf(ServiceAuthenticationException.class);
    verify(issuer).issue(request);
  }
}
