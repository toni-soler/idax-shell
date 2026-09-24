package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.repository.admin.TenantSearchGlobalRepository;
import es.idynamicsax.idax.repository.admin.TenantSetEnabledRepository;
import es.idynamicsax.idax.repository.admin.TenantUpdateNameRepository;
import es.idynamicsax.idax.service.admin.TenantOnboardingService;
import es.idynamicsax.idax.service.admin.dto.TenantOnboardingRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Wraps idax-core's own TenantOnboardingService/TenantSearchGlobalRepository/
 * TenantSetEnabledRepository/TenantUpdateNameRepository - the same tenant_create() path
 * LocalDemoInitializer already uses to bootstrap the very first tenant - previously never exposed
 * through any idax-shell endpoint or admin screen. Lets an admin spin up a genuinely separate
 * tenant (its own STIR marketplace, its own osTRIS economic community - everything is tenant_id
 * scoped under RLS) for test/pilot data instead of co-mingling it with production data under
 * PRUEBA labels in the same tenant.
 */
@RestController
@RequestMapping("/api/shell/v1/platform/tenants")
public class TenantAdminController {
  private final TenantSearchGlobalRepository search;
  private final TenantOnboardingService onboarding;
  private final TenantSetEnabledRepository setEnabledRepository;
  private final TenantUpdateNameRepository updateNameRepository;

  public TenantAdminController(TenantSearchGlobalRepository search, TenantOnboardingService onboarding,
      TenantSetEnabledRepository setEnabledRepository, TenantUpdateNameRepository updateNameRepository) {
    this.search = search; this.onboarding = onboarding;
    this.setEnabledRepository = setEnabledRepository; this.updateNameRepository = updateNameRepository;
  }

  @GetMapping
  @PreAuthorize("authentication.principal.superuser")
  public List<TenantAdminView> list() {
    return search.search(null, null, null, 0, 500).rows().stream().map(TenantAdminView::of).toList();
  }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("authentication.principal.superuser")
  public TenantAdminView create(@Valid @RequestBody TenantCreateRequest request) {
    // dataAreaId is an AX4-legacy-integration concept idax-core's onboarding contract requires
    // unconditionally (exactly 3 characters) - irrelevant to a STIR-only tenant with no AX4 side,
    // so it's derived here rather than asked of the admin creating a plain marketplace tenant.
    String dataAreaId = dataAreaIdFrom(request.tenantCode());
    var response = onboarding.createOrBootstrapTenant(TenantOnboardingRequest.builder()
        .tenantCode(request.tenantCode().trim())
        .tenantName(request.tenantName().trim())
        .adminSubject(request.adminEmail().trim())
        .adminEmail(request.adminEmail().trim())
        .adminDisplayName(request.adminDisplayName().trim())
        .adminAuthProvider("local")
        .adminPassword(request.adminPassword())
        .tenantRole("owner")
        .dataAreaId(dataAreaId)
        .dataAreaName(request.tenantName().trim())
        .build());
    return new TenantAdminView(response.tenantId(), response.tenantCode(), request.tenantName().trim(), true, false, OffsetDateTime.now());
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

  static String dataAreaIdFrom(String tenantCode) {
    String alnum = tenantCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    String padded = (alnum + "XXX").substring(0, 3);
    return padded;
  }

  public record TenantCreateRequest(
      @NotBlank String tenantCode, @NotBlank String tenantName,
      @NotBlank @Email String adminEmail, @NotBlank String adminDisplayName, @NotBlank String adminPassword) {}
}
