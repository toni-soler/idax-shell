package io.github.tonisoler.idaxshell.auth;

import io.github.tonisoler.idaxshell.config.ShellProperties;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "idax.shell.local-demo", name = "enabled", havingValue = "true")
public class LocalDemoInitializer implements ApplicationRunner {
  static final UUID TENANT_ID = UUID.fromString("018f6f9a-7b1c-7a2b-8c3d-4e5f60719001");
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final ShellProperties.LocalDemo demo;

  public LocalDemoInitializer(JdbcTemplate jdbc, PasswordEncoder passwords, ShellProperties properties) {
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.demo = properties.localDemo();
  }

  @Override @Transactional
  public void run(ApplicationArguments ignored) {
    jdbc.update("""
        insert into idax_core.tenant(tenant_id, code, name, status)
        values (?, ?, ?, 'active')
        on conflict (code) do update set name=excluded.name, status='active'
        """, TENANT_ID, demo.tenantCode(), demo.tenantName());
    jdbc.update("""
        update idax_core.app_user
           set email=?, display_name=?, auth_provider='local', is_active=true, is_superuser=true
         where external_subject='local:admin'
        """, demo.email(), demo.displayName());
    UUID userId = jdbc.queryForObject(
        "select user_id from idax_core.app_user where external_subject='local:admin'", UUID.class);
    jdbc.update("""
        insert into idax_core.app_user_credential(user_id, password_hash, password_algo)
        values (?, ?, 'bcrypt')
        on conflict (user_id) do update set password_hash=excluded.password_hash, password_algo='bcrypt'
        """, userId, passwords.encode(demo.password()));
    jdbc.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, TENANT_ID.toString());
    jdbc.update("""
        insert into idax_core.tenant_user(tenant_id, user_id, role)
        values (?, ?, 'owner')
        on conflict (tenant_id, user_id) do update set role='owner'
        """, TENANT_ID, userId);
  }
}
