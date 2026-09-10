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
    var existing = jdbc.queryForList("select tenant_id from idax_core.tenant where code=?", UUID.class, demo.tenantCode());
    UUID tenantId = existing.isEmpty()
        ? jdbc.queryForObject("select tenant_id from idax_core.tenant_create(?, ?, 'active', false)", UUID.class, demo.tenantCode(), demo.tenantName())
        : existing.getFirst();
    jdbc.update("""
        update idax_core.app_user
           set email=?, display_name=?, auth_provider='local', is_active=true, is_superuser=true
         where external_subject=?
        """, demo.email(), demo.displayName(), LocalIdentitySubjectPolicy.BREAK_GLASS_SUBJECT);
    UUID userId = jdbc.queryForObject(
        "select user_id from idax_core.app_user where external_subject=?", UUID.class,
        LocalIdentitySubjectPolicy.BREAK_GLASS_SUBJECT);
    jdbc.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, tenantId.toString());
    jdbc.update("""
        delete from idax_core.tenant_user
         where tenant_id=?
           and user_id in (
             select user_id from idax_core.app_user
              where external_subject='local:admin' and email=?
           )
        """, tenantId, demo.email());
    jdbc.update("""
        insert into idax_core.tenant_user(tenant_id, user_id, role)
        values (?, ?, 'owner')
        on conflict (tenant_id, user_id) do update set role='owner'
        """, tenantId, userId);
  }
}
