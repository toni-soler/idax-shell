package io.github.tonisoler.idaxshell.serviceauth;

import es.idynamicsax.idax.service.auth.ServiceTokenIssuer;
import es.idynamicsax.idax.service.auth.ServiceTokenRequest;
import es.idynamicsax.idax.service.auth.ServiceTokenResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 client-credentials token exchange for machine callers (e.g. osTRIS's ledger delivery
 * worker requesting a token for the idax-ledger audience). This is a thin HTTP wrapper: every
 * credential/tenant/audience/grant check and the JWT issuance itself live in idax-core's
 * ServiceTokenIssuer, already scanned and wired by IdaxShellApplication - nothing here
 * duplicates or re-implements that logic.
 *
 * SecurityConfiguration permits POST to this one path anonymously (see the comment there): the
 * request body itself carries the credentials that authenticate the caller, the same pattern
 * already used for /api/shell/v1/auth/login. Only {@code issuer-enabled=true} (idax.service-auth)
 * brings this controller's single dependency, ServiceTokenIssuer, into existence at all - with it
 * unset/false (the default), neither bean exists and this endpoint is absent, not merely blocked.
 */
@RestController
@RequestMapping("/api/service-auth")
@ConditionalOnProperty(prefix = "idax.service-auth", name = "issuer-enabled", havingValue = "true")
public class ServiceAuthController {
  private final ServiceTokenIssuer issuer;

  public ServiceAuthController(ServiceTokenIssuer issuer) {
    this.issuer = issuer;
  }

  @PostMapping("/token")
  public ServiceTokenResponse token(@RequestBody ServiceTokenRequest request) {
    return issuer.issue(request);
  }
}
