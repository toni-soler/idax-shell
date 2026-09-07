package io.github.tonisoler.idaxshell.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/shell/v1")
@ConditionalOnProperty(prefix = "idax.shell.local-demo", name = "enabled", havingValue = "true")
public class AuthenticationController {
  private final LocalAuthenticationService authentication;
  public AuthenticationController(LocalAuthenticationService authentication) { this.authentication = authentication; }

  @PostMapping("/auth/login")
  public LocalAuthenticationService.Session login(@Valid @RequestBody LoginRequest request) {
    try { return authentication.login(request.email(), request.password()); }
    catch (org.springframework.security.core.AuthenticationException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
  }

  @GetMapping("/session")
  public LocalAuthenticationService.Session session(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
    var user = new LocalAuthenticationService.User(userId, jwt.getClaimAsString("email"),
        jwt.getClaimAsString("displayName"), Boolean.TRUE.equals(jwt.getClaim("superuser")));
    List<LocalAuthenticationService.Tenant> tenants = authentication.tenants(userId);
    return new LocalAuthenticationService.Session(null, user, tenants);
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
}
