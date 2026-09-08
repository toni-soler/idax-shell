package io.github.tonisoler.idaxshell.auth;

import es.idynamicsax.idax.domain.AppUser;
import es.idynamicsax.idax.repository.AppUserRepository;
import es.idynamicsax.idax.repository.TenantRepository;
import es.idynamicsax.idax.repository.TenantUserRepository;
import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.security.TokenValidator;
import es.idynamicsax.idax.service.auth.LocalAuthService;
import es.idynamicsax.idax.service.auth.LoginOutcome;
import es.idynamicsax.idax.service.auth.MfaForcedSetupService;
import es.idynamicsax.idax.service.auth.MfaLoginService;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/** Public transport adapter over the Core LOCAL authentication contract. */
@Service
@ConditionalOnProperty(prefix = "idax.shell.local-demo", name = "enabled", havingValue = "true")
public class LocalAuthenticationService {
  private final LocalAuthService coreAuthentication;
  private final TokenValidator tokenValidator;
  private final AppUserRepository users;
  private final TenantUserRepository memberships;
  private final TenantRepository tenants;
  private final MfaLoginService mfaLogin;
  private final MfaForcedSetupService forcedSetup;

  public LocalAuthenticationService(LocalAuthService coreAuthentication, TokenValidator tokenValidator,
                                    AppUserRepository users, TenantUserRepository memberships,
                                    TenantRepository tenants, MfaLoginService mfaLogin,
                                    MfaForcedSetupService forcedSetup) {
    this.coreAuthentication = coreAuthentication;
    this.tokenValidator = tokenValidator;
    this.users = users;
    this.memberships = memberships;
    this.tenants = tenants;
    this.mfaLogin = mfaLogin;
    this.forcedSetup = forcedSetup;
  }

  public Object login(String email, String password) {
    LoginOutcome outcome;
    try {
      outcome = coreAuthentication.login(email, password);
    } catch (IllegalArgumentException exception) {
      throw new BadCredentialsException("Invalid credentials", exception);
    }
    return switch (outcome) {
      case LoginOutcome.Success success -> sessionFrom(success.response());
      case LoginOutcome.MfaRequired required -> new MfaRequired("MFA_REQUIRED", required.challengeToken());
      case LoginOutcome.MfaSetupRequired required -> new MfaSetupRequired("MFA_SETUP_REQUIRED", required.setupToken());
    };
  }

  public Session session(CurrentUser currentUser) {
    if (currentUser == null || currentUser.isService() || currentUser.getUserId() == null) {
      throw new BadCredentialsException("A validated user identity is required");
    }
    AppUser user = users.findById(currentUser.getUserId())
        .orElseThrow(() -> new BadCredentialsException("Authenticated user no longer exists"));
    return new Session(null, null, currentUser.getRoles().stream().sorted().toList(),
        toUser(user), tenantViews(user.getId()));
  }

  public Session verifyMfa(String challengeToken, String code) {
    return sessionFrom(mfaLogin.verify(challengeToken, code));
  }

  public Object startForcedSetup(String setupToken) { return forcedSetup.start(setupToken); }

  public ForcedSetupActivation activateForcedSetup(String setupToken, String code) {
    var result = forcedSetup.activate(setupToken, code);
    return new ForcedSetupActivation(result.recoveryCodes().codes(),
        result.recoveryCodes().generatedAt(), sessionFrom(result.tokens()));
  }

  private Session sessionFrom(LocalAuthService.LoginResponse tokens) {
    var validation = tokenValidator.validateAndExtract(tokens.accessToken());
    if (!validation.isValid() || validation.getCurrentUser() == null) {
      throw new IllegalStateException("Core issued an access token that its validator rejected");
    }
    CurrentUser currentUser = validation.getCurrentUser();
    AppUser user = users.findById(currentUser.getUserId())
        .orElseThrow(() -> new IllegalStateException("Core issued a token for an unknown user"));
    return new Session(tokens.accessToken(), tokens.refreshToken(), tokens.roles(),
        toUser(user), tenantViews(user.getId()));
  }

  private List<TenantView> tenantViews(UUID userId) {
    return tenants.findAllById(memberships.findTenantIdsByUserId(userId)).stream()
        .filter(tenant -> "active".equalsIgnoreCase(tenant.getStatus()))
        .map(tenant -> new TenantView(tenant.getId(), tenant.getCode(), tenant.getName()))
        .sorted(java.util.Comparator.comparing(TenantView::name))
        .toList();
  }

  private User toUser(AppUser user) {
    return new User(user.getId(), user.getEmail(), user.getDisplayName(), Boolean.TRUE.equals(user.getIsSuperuser()));
  }

  public record User(UUID id, String email, String displayName, boolean superuser) {}
  public record TenantView(UUID id, String code, String name) {}
  public record Session(String accessToken, String refreshToken, List<String> roles, User user, List<TenantView> tenants) {}
  public record MfaRequired(String status, String challengeToken) {}
  public record MfaSetupRequired(String status, String setupToken) {}
  public record ForcedSetupActivation(List<String> recoveryCodes,
      java.time.OffsetDateTime generatedAt, Session session) {}
}
