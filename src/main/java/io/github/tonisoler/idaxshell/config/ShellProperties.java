package io.github.tonisoler.idaxshell.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("idax.shell")
public record ShellProperties(Resource bootstrapTokenFile, Resource extensionManifest, LocalDemo localDemo) {
  public record LocalDemo(boolean enabled, String email, String password, String displayName,
                          String tenantCode, String tenantName) {}
}
