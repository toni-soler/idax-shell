package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.repository.admin.TenantSearchGlobalRepository;
import es.idynamicsax.idax.repository.admin.TenantSearchGlobalRepository.TenantRow;
import es.idynamicsax.idax.tenant.TenantContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Brings every workspace's template roles up to date: once when the Shell is ready, then every
 * {@code idax.shell.role-templates.reconcile-interval} (PT15M by default). Workspace creation and
 * member creation also reconcile, so this mainly catches existing workspaces after a module upgrade,
 * and permissions a module registered in the Core catalog after the Shell started.
 *
 * Runs on its own single thread rather than @EnableScheduling, which would also wake every Core
 * @Scheduled bean the Shell does not run today. Each workspace is reconciled in its own
 * transaction with the same elevation TenantAdminController.create uses (SET LOCAL ROLE idax_admin
 * + idax_core.set_tenant + TenantContext): there is no signed-in user here. One workspace failing
 * is logged and never stops the others.
 */
@Component
public class RoleTemplateReconciler {
  private static final Logger log = LoggerFactory.getLogger(RoleTemplateReconciler.class);
  private static final int PAGE = 500;
  private final RoleTemplateService templates;
  private final TenantSearchGlobalRepository tenants;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final Duration interval;
  private ScheduledExecutorService executor;

  public RoleTemplateReconciler(RoleTemplateService templates, TenantSearchGlobalRepository tenants, JdbcTemplate jdbc,
      PlatformTransactionManager transactionManager,
      @Value("${idax.shell.role-templates.reconcile-interval:PT15M}") Duration interval) {
    this.templates = templates; this.tenants = tenants; this.jdbc = jdbc;
    this.transactions = new TransactionTemplate(transactionManager); this.interval = interval;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    if (!templates.hasTemplates() || interval.isZero() || interval.isNegative()) return;
    executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "role-template-reconciler");
      thread.setDaemon(true);
      return thread;
    });
    executor.scheduleWithFixedDelay(this::reconcileAll, 0, interval.toSeconds(), TimeUnit.SECONDS);
  }

  @jakarta.annotation.PreDestroy
  public void stop() {
    if (executor != null) executor.shutdownNow();
  }

  /** Returns how many workspaces were reconciled without error. */
  public int reconcileAll() {
    int done = 0;
    try {
      for (TenantRow tenant : allTenants()) {
        try {
          inTenant(tenant, () -> templates.reconcile(tenant.tenantId()));
          done++;
        } catch (RuntimeException failure) {
          log.warn("Role templates could not be reconciled in tenant {}: {}", tenant.code(), failure.getMessage());
        }
      }
    } catch (RuntimeException failure) {
      log.warn("Role templates could not list the workspaces: {}", failure.getMessage());
    }
    return done;
  }

  private List<TenantRow> allTenants() {
    List<TenantRow> all = new ArrayList<>();
    for (int offset = 0; ; offset += PAGE) {
      int from = offset;
      List<TenantRow> page = transactions.execute(status -> {
        jdbc.execute("SET LOCAL ROLE idax_admin");
        // tenant_search_global(p_code, p_name, p_enabled, p_limit, p_offset): limit before offset.
        return tenants.search(null, null, null, PAGE, from).rows();
      });
      if (page == null || page.isEmpty()) return all;
      all.addAll(page);
      if (page.size() < PAGE) return all;
    }
  }

  private void inTenant(TenantRow tenant, Runnable work) {
    transactions.executeWithoutResult(status -> {
      jdbc.execute("SET LOCAL ROLE idax_admin");
      jdbc.execute("SELECT idax_core.set_tenant(?)", (java.sql.PreparedStatement statement) -> {
        statement.setObject(1, tenant.tenantId()); statement.execute(); return null;
      });
      TenantContext.set(new TenantContext(tenant.tenantId(), tenant.code(), null, "idax-shell:role-templates",
          TenantContext.DbRole.IDAX_ADMIN));
      try {
        work.run();
      } finally {
        TenantContext.clear();
      }
    });
  }
}
