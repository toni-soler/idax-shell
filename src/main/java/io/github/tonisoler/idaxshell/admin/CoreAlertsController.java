package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.alerts.AlertDefinitionService;
import es.idynamicsax.idax.alerts.AlertRunnerService;
import es.idynamicsax.idax.alerts.SavedFilterAlertService;
import es.idynamicsax.idax.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static es.idynamicsax.idax.alerts.dto.AlertDtos.*;
import static es.idynamicsax.idax.alerts.dto.SavedFilterAlertDtos.*;

/** Distinct public contracts for tenant alert definitions and saved-filter alert requests. */
@RestController
@RequestMapping("/api/shell/v1/tenants/{tenantId}/core-alerts")
public class CoreAlertsController {
  private final AlertDefinitionService definitions;
  private final AlertRunnerService definitionRunner;
  private final SavedFilterAlertService savedFilters;

  public CoreAlertsController(AlertDefinitionService definitions, AlertRunnerService definitionRunner,
      SavedFilterAlertService savedFilters) {
    this.definitions = definitions; this.definitionRunner = definitionRunner; this.savedFilters = savedFilters;
  }

  @GetMapping
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.read')")
  public List<AlertDefinitionView> catalog(@PathVariable UUID tenantId) { return definitions.catalog(); }

  @GetMapping("/{alertCode}")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.read')")
  public AlertDefinitionView get(@PathVariable UUID tenantId, @PathVariable String alertCode) { return definitions.get(alertCode); }

  @PutMapping("/{alertCode}")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.manage')")
  public AlertDefinitionView update(@PathVariable UUID tenantId, @PathVariable String alertCode,
      @Valid @RequestBody AlertDefinitionUpdateRequest request) { return definitions.update(alertCode, request); }

  @PostMapping("/{alertCode}/run-now") @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.manage')")
  public void runNow(@PathVariable UUID tenantId, @PathVariable String alertCode) { definitionRunner.runNow(alertCode); }

  @GetMapping("/{alertCode}/run-log")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.read')")
  public Page<AlertRunLogView> runLog(@PathVariable UUID tenantId, @PathVariable String alertCode,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
    return definitions.runLog(alertCode, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
  }

  @GetMapping("/saved-filters/supported-entities")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.request')")
  public List<String> supportedEntities(@PathVariable UUID tenantId) { return savedFilters.supportedEntities(); }

  @GetMapping("/saved-filters/pending")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.manage')")
  public List<SavedFilterAlertView> pending(@PathVariable UUID tenantId) { return savedFilters.listPending(); }

  @PostMapping("/saved-filters/{id}/decide")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.manage')")
  public SavedFilterAlertView decide(@PathVariable UUID tenantId, @PathVariable UUID id,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody SavedFilterAlertDecisionRequest request) {
    return savedFilters.decide(currentUser, id, request);
  }
}
