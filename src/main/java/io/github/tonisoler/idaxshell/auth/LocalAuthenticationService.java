package io.github.tonisoler.idaxshell.auth;

import io.github.tonisoler.idaxshell.config.ShellProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "idax.shell.local-demo", name = "enabled", havingValue = "true")
public class LocalAuthenticationService {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final ShellProperties properties;

  public LocalAuthenticationService(JdbcTemplate jdbc, PasswordEncoder passwords,
                                    JwtEncoder encoder, ShellProperties properties) {
    this.jdbc = jdbc; this.passwords = passwords; this.encoder = encoder; this.properties = properties;
  }

  public Session login(String email, String password) {
    var users = jdbc.query("""
        select u.user_id, u.email, u.display_name, u.is_superuser, c.password_hash
          from idax_core.app_user u join idax_core.app_user_credential c on c.user_id=u.user_id
         where lower(u.email)=lower(?) and u.auth_provider='local' and u.is_active=true
        """, (rs, row) -> new UserRow(rs.getObject("user_id", UUID.class), rs.getString("email"),
            rs.getString("display_name"), rs.getBoolean("is_superuser"), rs.getString("password_hash")), email);
    if (users.size() != 1 || !passwords.matches(password, users.getFirst().passwordHash()))
      throw new BadCredentialsException("Invalid credentials");
    UserRow user = users.getFirst();
    List<Tenant> tenants = tenants(user.id());
    if (tenants.isEmpty()) throw new BadCredentialsException("User has no active workspace");
    return issue(user, tenants);
  }

  public Session issue(UserRow user, List<Tenant> tenants) {
    Instant now = Instant.now();
    Tenant active = tenants.getFirst();
    JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.jwt().issuer())
        .issuedAt(now).expiresAt(now.plus(properties.jwt().accessTokenTtl()))
        .subject(user.id().toString()).claim("userId", user.id().toString())
        .claim("tenantId", active.id().toString()).claim("roles", List.of("owner"))
        .claim("superuser", user.superuser()).claim("email", user.email())
        .claim("displayName", user.displayName()).build();
    String token = encoder.encode(JwtEncoderParameters.from(
        JwsHeader.with(SignatureAlgorithm.RS256).keyId("idax-shell").build(), claims)).getTokenValue();
    return new Session(token, new User(user.id(), user.email(), user.displayName(), user.superuser()), tenants);
  }

  public List<Tenant> tenants(UUID userId) {
    return jdbc.query("""
        select t.tenant_id, t.code, t.name from idax_core.tenant t
        join idax_core.tenant_user tu on tu.tenant_id=t.tenant_id
        where tu.user_id=? and t.status='active' order by t.name
        """, (rs, row) -> new Tenant(rs.getObject("tenant_id", UUID.class), rs.getString("code"), rs.getString("name")), userId);
  }

  record UserRow(UUID id, String email, String displayName, boolean superuser, String passwordHash) {}
  public record LoginRequest(String email, String password) {}
  public record User(UUID id, String email, String displayName, boolean superuser) {}
  public record Tenant(UUID id, String code, String name) {}
  public record Session(String accessToken, User user, List<Tenant> tenants) {}
}
