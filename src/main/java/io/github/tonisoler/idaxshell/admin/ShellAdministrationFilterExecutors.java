package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.alerts.filters.GenericFilterExecutor;
import es.idynamicsax.idax.service.permission.RolePermissionService;
import es.idynamicsax.idax.service.tenant.TenantUserService;
import es.idynamicsax.idax.tenant.TenantContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers Shell-owned filter replay without duplicating Core persistence. */
@Configuration
public class ShellAdministrationFilterExecutors {
  @Bean GenericFilterExecutor shellUsersFilterExecutor(TenantUserService service) {
    return executor("users", filters -> service.findAll(tenantId()).stream().map(user -> mapOf(
        "userId", user.userId(), "displayName", user.displayName(), "email", user.email(),
        "role", user.role(), "enabled", user.enabled())).toList());
  }

  @Bean GenericFilterExecutor shellRolesFilterExecutor(RolePermissionService service) {
    return executor("roles", filters -> service.listRoles(tenantId()).stream().map(role -> mapOf(
        "id", role.id(), "key", role.key(), "name", role.name(), "description", role.description(),
        "permissionCount", role.permissionCount(), "enabled", role.enabled())).toList());
  }

  private GenericFilterExecutor executor(String key,
      java.util.function.Function<Map<String, String>, List<Map<String, Object>>> source) {
    return new GenericFilterExecutor() {
      @Override public String entityKey() { return key; }
      @Override public List<Map<String, Object>> query(Map<String, String> filters, int maxRows) {
        return source.apply(filters).stream().filter(row -> matches(row, filters)).limit(maxRows).toList();
      }
    };
  }

  private static boolean matches(Map<String, Object> row, Map<String, String> filters) {
    if (filters == null) return true;
    return filters.entrySet().stream().allMatch(entry -> String.valueOf(row.getOrDefault(entry.getKey(), ""))
        .toLowerCase(Locale.ROOT).contains(String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT)));
  }

  private static Map<String, Object> mapOf(Object... values) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
    return result;
  }

  private static java.util.UUID tenantId() {
    TenantContext context = TenantContext.get();
    if (context == null || context.getTenantId() == null) throw new IllegalStateException("Tenant context is required");
    return context.getTenantId();
  }
}
