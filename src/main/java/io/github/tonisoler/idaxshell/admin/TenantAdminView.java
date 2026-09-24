package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.repository.admin.TenantSearchGlobalRepository;
import es.idynamicsax.idax.repository.admin.TenantSetEnabledRepository;
import es.idynamicsax.idax.repository.admin.TenantUpdateNameRepository;
import java.time.OffsetDateTime;
import java.util.UUID;

/** idax-core's TenantSearchGlobalRepository/TenantSetEnabledRepository/TenantUpdateNameRepository
 * each declare their own structurally-identical but unrelated TenantRow record (one per JDBC
 * repository interface) - this is the one shape the frontend actually sees, with `status`
 * collapsed to the plain `enabled` boolean CrudWorkspace-style editors already expect. */
public record TenantAdminView(UUID id, String code, String name, boolean enabled, boolean mfaRequired, OffsetDateTime createdAt) {
  static TenantAdminView of(TenantSearchGlobalRepository.TenantRow row) {
    return new TenantAdminView(row.tenantId(), row.code(), row.name(), "active".equalsIgnoreCase(row.status()), row.mfaRequired(), row.createdAt());
  }
  static TenantAdminView of(TenantSetEnabledRepository.TenantRow row) {
    return new TenantAdminView(row.tenantId(), row.code(), row.name(), "active".equalsIgnoreCase(row.status()), row.mfaRequired(), row.createdAt());
  }
  static TenantAdminView of(TenantUpdateNameRepository.TenantRow row) {
    return new TenantAdminView(row.tenantId(), row.code(), row.name(), "active".equalsIgnoreCase(row.status()), row.mfaRequired(), row.createdAt());
  }
}
