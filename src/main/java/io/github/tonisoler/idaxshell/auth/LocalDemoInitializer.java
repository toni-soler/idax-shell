package io.github.tonisoler.idaxshell.auth;

import io.github.tonisoler.idaxshell.config.ShellProperties;
import es.idynamicsax.idax.service.auth.BreakGlassIdentityService;
import es.idynamicsax.idax.service.auth.LocalIdentitySubjectPolicy;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "idax.shell.local-demo", name = "enabled", havingValue = "true")
public class LocalDemoInitializer implements ApplicationRunner {
  static final UUID TENANT_ID = UUID.fromString("018f6f9a-7b1c-7a2b-8c3d-4e5f60719001");
  private final JdbcTemplate jdbc;
  private final BreakGlassIdentityService breakGlassIdentity;
  private final TransactionTemplate transaction;
  private final ShellProperties.LocalDemo demo;

  public LocalDemoInitializer(JdbcTemplate jdbc, BreakGlassIdentityService breakGlassIdentity,
                              PlatformTransactionManager transactionManager, ShellProperties properties) {
    this.jdbc = jdbc;
    this.breakGlassIdentity = breakGlassIdentity;
    this.transaction = new TransactionTemplate(transactionManager);
    this.demo = properties.localDemo();
  }

  @Override
  public void run(ApplicationArguments ignored) {
    breakGlassIdentity.provision();
    transaction.executeWithoutResult(status -> initializeWorkspace());
  }

  private void initializeWorkspace() {
    jdbc.update("""
        insert into idax_core.tenant(tenant_id, code, name, status)
        values (?, ?, ?, 'active')
        on conflict (code) do update set name=excluded.name, status='active'
        """, TENANT_ID, demo.tenantCode(), demo.tenantName());
    jdbc.update("""
        update idax_core.app_user
           set email=?, display_name=?, auth_provider='local', is_active=true, is_superuser=true
         where external_subject=?
        """, demo.email(), demo.displayName(), LocalIdentitySubjectPolicy.BREAK_GLASS_SUBJECT);
    UUID userId = jdbc.queryForObject(
        "select user_id from idax_core.app_user where external_subject=?", UUID.class,
        LocalIdentitySubjectPolicy.BREAK_GLASS_SUBJECT);
    jdbc.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, TENANT_ID.toString());
    jdbc.update("""
        delete from idax_core.tenant_user
         where tenant_id=?
           and user_id in (
             select user_id from idax_core.app_user
              where external_subject='local:admin' and email=?
           )
        """, TENANT_ID, demo.email());
    jdbc.update("""
        insert into idax_core.tenant_user(tenant_id, user_id, role)
        values (?, ?, 'owner')
        on conflict (tenant_id, user_id) do update set role='owner'
        """, TENANT_ID, userId);
  }
}
