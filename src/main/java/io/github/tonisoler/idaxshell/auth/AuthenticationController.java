package io.github.tonisoler.idaxshell.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import es.idynamicsax.idax.security.CurrentUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shell/v1")
@ConditionalOnProperty(prefix = "idax.shell.local-demo", name = "enabled", havingValue = "true")
public class AuthenticationController {
  private final LocalAuthenticationService authentication;
  public AuthenticationController(LocalAuthenticationService authentication) { this.authentication = authentication; }

  // Failure classification (INVALID_CREDENTIALS vs RATE_LIMITED, both with distinct HTTP statuses)
  // lives in AuthenticationExceptionHandler, not here - see that class for why: idax-core's own
  // LocalCredentialAuthenticator deliberately collapses every credential-related failure (unknown
  // email, wrong password, disabled account) into one IllegalArgumentException("INVALID_CREDENTIALS"),
  // by design, to avoid leaking account existence/status. RateLimitedException is a genuinely
  // distinct, safe-to-reveal case that previously fell through uncaught to a bare 500.
  @PostMapping("/auth/login")
  public Object login(@Valid @RequestBody LoginRequest request) {
    return authentication.login(request.email(), request.password());
  }

  @GetMapping("/session")
  public LocalAuthenticationService.Session session(
      @org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser) {
    return authentication.session(currentUser);
  }

  @PostMapping("/auth/mfa/verify")
  public LocalAuthenticationService.Session verifyMfa(@RequestBody MfaVerifyRequest request) {
    return authentication.verifyMfa(request.challengeToken(), request.code());
  }

  @PostMapping("/auth/mfa/setup-required/start")
  public Object startForcedSetup(@RequestBody ForcedSetupStartRequest request) {
    return authentication.startForcedSetup(request.setupToken());
  }

  @PostMapping("/auth/mfa/setup-required/activate")
  public LocalAuthenticationService.ForcedSetupActivation activateForcedSetup(
      @RequestBody ForcedSetupActivateRequest request) {
    return authentication.activateForcedSetup(request.setupToken(), request.code());
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
  public record MfaVerifyRequest(@NotBlank String challengeToken, @NotBlank String code) {}
  public record ForcedSetupStartRequest(@NotBlank String setupToken) {}
  public record ForcedSetupActivateRequest(@NotBlank String setupToken, @NotBlank String code) {}
}
