package io.github.tonisoler.idaxshell.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("idax.shell")
public record ShellProperties(Resource bootstrapTokenFile, Resource extensionManifest, Jwt jwt) {
  public record Jwt(Resource privateKey, Resource publicKey, String issuer, Duration accessTokenTtl) {}
}
