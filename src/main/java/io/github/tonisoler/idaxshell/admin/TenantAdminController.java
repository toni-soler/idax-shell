package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.repository.admin.TenantCreateRepository;
import es.idynamicsax.idax.repository.admin.TenantSearchGlobalRepository;
import es.idynamicsax.idax.repository.admin.TenantSetEnabledRepository;
import es.idynamicsax.idax.repository.admin.TenantUpdateNameRepository;
import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.service.tenant.TenantUserService;
import es.idynamicsax.idax.service.tenant.dto.TenantUserRequest;
import es.idynamicsax.idax.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Platform-level tenant management - genuinely cross-tenant, unlike every other endpoint in
 * AdministrationController (which all hang off an already-known {tenantId} the caller belongs
 * to). There is no tenant context yet when you're creating one, and no per-tenant role should be
 * able to spin up or disable a DIFFERENT tenant - so this is gated on the platform superuser flag
 * itself (authentication.principal is the CurrentUser set by idax-core's shared JwtAuthFilter),
 * never a per-tenant permission code.
 *
 * create() deliberately does NOT use idax-core's own TenantOnboardingService.createOrBootstrapTenant,
 * despite that looking like the obvious fit - it unconditionally calls ensureDataArea(), which
 * requires an "idax.dataarea" table (AX4-legacy-integration schema) that STIR's deployment never
 * provisions, since STIR has no AX4 integration. Confirmed live (25/09/2026): calling it threw
 * BadSqlGrammarException - relation "idax.dataarea" does not exist - for both
 * createOrBootstrapTenant and its bootstrapExistingTenant sibling (same ensureDataArea() call).
 * Composed instead from TenantCreateRepository (tenant row only, no DataArea) and TenantUserService
 * (the same service AdministrationController.createUser already uses successfully for ordinary
 * tenant users, including local passwords - proven all session, never touches DataArea).
 */
@RestController
@RequestMapping("/api/shell/v1/platform/tenants")
public class TenantAdminController {
  private final TenantSearchGlobalRepository search;
  private final TenantCreateRepository create;
  private final TenantUserService tenantUsers;
  private final TenantSetEnabledRepository setEnabledRepository;
  private final TenantUpdateNameRepository updateNameRepository;
  private final JdbcTemplate jdbcTemplate;

  public TenantAdminController(TenantSearchGlobalRepository search, TenantCreateRepository create,
      TenantUserService tenantUsers, TenantSetEnabledRepository setEnabledRepository,
      TenantUpdateNameRepository updateNameRepository, JdbcTemplate jdbcTemplate) {
    this.search = search; this.create = create; this.tenantUsers = tenantUsers;
    this.setEnabledRepository = setEnabledRepository; this.updateNameRepository = updateNameRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping
  @PreAuthorize("authentication.principal.superuser")
  public List<TenantAdminView> list() {
    // idax_core.tenant_search_global(p_code, p_name, p_enabled, p_limit, p_offset) - limit before
    // offset. Swapping them (as a first version of this method did) isn't a SQL error - LIMIT 0
    // OFFSET 500 is syntactically valid, so it silently returns zero rows with no exception
    // anywhere in the stack, exactly matching what shipped: an empty list, no error, no log line.
    return search.search(null, null, null, 500, 0).rows().stream().map(TenantAdminView::of).toList();
  }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("authentication.principal.superuser")
  @Transactional
  public TenantAdminView create(@Valid @RequestBody TenantCreateRequest request, @AuthenticationPrincipal CurrentUser caller) {
    var tenant = create.create(request.tenantCode().trim(), request.tenantName().trim(), "active", false);
    // TenantUserService.create() guards TenantContext.get().tenantId == the tenantId argument
    // (SecurityException("Tenant mismatch") otherwise) - correct for its normal callers (always
    // an already-resolved tenant the caller belongs to), but this tenant didn't exist a moment
    // ago, so there is no resolved context for it yet. Elevate explicitly for the rest of this
    // transaction, mirroring idax_core's own bootstrap pattern (TenantOnboardingService also does
    // exactly this SET LOCAL ROLE + set_tenant dance before its own cross-tenant writes).
    jdbcTemplate.execute("SET LOCAL ROLE idax_admin");
    jdbcTemplate.execute("SELECT idax_core.set_tenant(?)", (java.sql.PreparedStatement ps) -> {
      ps.setObject(1, tenant.tenantId()); ps.execute(); return null;
    });
    TenantContext.set(new TenantContext(tenant.tenantId(), tenant.code(), caller.getUserId(), caller.getUsername(), TenantContext.DbRole.IDAX_ADMIN));
    try {
      tenantUsers.create(tenant.tenantId(), new TenantUserRequest(
          request.adminEmail().trim(), request.adminEmail().trim(), request.adminDisplayName().trim(),
          "local", request.adminPassword(), "owner", Boolean.TRUE, Boolean.FALSE));
    } finally {
      TenantContext.clear();
    }
    return new TenantAdminView(tenant.tenantId(), tenant.code(), tenant.name(), true, tenant.mfaRequired(), tenant.createdAt());
  }

  @PutMapping("/{tenantId}/enabled")
  @PreAuthorize("authentication.principal.superuser")
  public TenantAdminView setEnabled(@PathVariable UUID tenantId, @RequestBody Map<String, Boolean> body) {
    return setEnabledRepository.setEnabled(tenantId, Boolean.TRUE.equals(body.get("enabled")))
        .map(TenantAdminView::of).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tenant not found"));
  }

  @PutMapping("/{tenantId}/name")
  @PreAuthorize("authentication.principal.superuser")
  public TenantAdminView updateName(@PathVariable UUID tenantId, @RequestBody Map<String, String> body) {
    return updateNameRepository.updateName(tenantId, body.get("name"))
        .map(TenantAdminView::of).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tenant not found"));
  }

  public record TenantCreateRequest(
      @NotBlank String tenantCode, @NotBlank String tenantName,
      @NotBlank @Email String adminEmail, @NotBlank String adminDisplayName, @NotBlank String adminPassword) {}
}
