package io.github.tonisoler.idaxshell.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.idynamicsax.idax.alerts.SavedFilterAlertRunnerService;
import es.idynamicsax.idax.alerts.SavedFilterAlertService;
import es.idynamicsax.idax.domain.UserSavedFilter;
import es.idynamicsax.idax.domain.UserSavedFilterPreference;
import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.service.me.UserPreferencesService;
import es.idynamicsax.idax.service.me.dto.UserPreferencesRequest;
import es.idynamicsax.idax.service.permission.RolePermissionService;
import es.idynamicsax.idax.service.permission.dto.PermissionInfo;
import es.idynamicsax.idax.service.permission.dto.RoleRequest;
import es.idynamicsax.idax.service.permission.dto.RoleSummary;
import es.idynamicsax.idax.service.permission.dto.RoleUserInfo;
import es.idynamicsax.idax.service.permission.dto.UserRolesRequest;
import es.idynamicsax.idax.service.tenant.TenantUserService;
import es.idynamicsax.idax.service.tenant.dto.TenantUserRequest;
import es.idynamicsax.idax.service.tenant.dto.TenantUserResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static es.idynamicsax.idax.alerts.dto.SavedFilterAlertDtos.*;

/** Thin public HTTP composition over Core application services. */
@RestController
@RequestMapping("/api/shell/v1/tenants/{tenantId}")
public class AdministrationController {
  private final TenantUserService users;
  private final RolePermissionService roles;
  private final UserPreferencesService preferences;
  private final SavedFilterAlertService alerts;
  private final SavedFilterAlertRunnerService alertRunner;
  private final ObjectMapper mapper;

  public AdministrationController(TenantUserService users, RolePermissionService roles,
      UserPreferencesService preferences, SavedFilterAlertService alerts,
      SavedFilterAlertRunnerService alertRunner, ObjectMapper mapper) {
    this.users = users; this.roles = roles; this.preferences = preferences;
    this.alerts = alerts; this.alertRunner = alertRunner; this.mapper = mapper;
  }

  @GetMapping("/users")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.users.read')")
  public List<TenantUserResponse> users(@PathVariable UUID tenantId) { return users.findAll(tenantId); }

  @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.users.create')")
  public TenantUserResponse createUser(@PathVariable UUID tenantId, @RequestBody TenantUserRequest request) {
    return users.create(tenantId, request);
  }

  @PutMapping("/users/{userId}")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.users.update')")
  public TenantUserResponse updateUser(@PathVariable UUID tenantId, @PathVariable UUID userId,
      @RequestBody TenantUserRequest request) { return users.update(tenantId, userId, request); }

  @DeleteMapping("/users/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.users.delete')")
  public void deleteUser(@PathVariable UUID tenantId, @PathVariable UUID userId) { users.delete(tenantId, userId); }

  @GetMapping("/roles")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.read')")
  public List<RoleSummary> roles(@PathVariable UUID tenantId) { return roles.listRoles(tenantId); }

  @PostMapping("/roles") @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.manage')")
  public RoleSummary createRole(@PathVariable UUID tenantId, @RequestBody RoleRequest request) { return roles.create(tenantId, request); }

  @PutMapping("/roles/{roleId}")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.manage')")
  public RoleSummary updateRole(@PathVariable UUID tenantId, @PathVariable UUID roleId, @RequestBody RoleRequest request) {
    return roles.update(tenantId, roleId, request);
  }

  @DeleteMapping("/roles/{roleId}") @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.manage')")
  public void deleteRole(@PathVariable UUID tenantId, @PathVariable UUID roleId) { roles.delete(tenantId, roleId); }

  @GetMapping("/roles/catalog")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.read')")
  public List<PermissionInfo> permissionCatalog(@PathVariable UUID tenantId) { return roles.permissionCatalog(); }

  @GetMapping("/roles/{roleId}/permissions")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.read')")
  public Set<String> rolePermissions(@PathVariable UUID tenantId, @PathVariable UUID roleId) {
    return roles.rolePermissions(tenantId, roleId);
  }

  @PutMapping("/roles/{roleId}/permissions")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.manage')")
  public Set<String> replaceRolePermissions(@PathVariable UUID tenantId, @PathVariable UUID roleId,
      @RequestBody Set<String> permissions) { return roles.replacePermissions(tenantId, roleId, permissions); }

  @GetMapping("/roles/users")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.read')")
  public List<RoleUserInfo> roleUsers(@PathVariable UUID tenantId) { return roles.listUsers(tenantId); }

  @PutMapping("/roles/users/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('system.roles.manage')")
  public void replaceUserRoles(@PathVariable UUID tenantId, @PathVariable UUID userId,
      @RequestBody UserRolesRequest request) { roles.replaceUserRoles(tenantId, userId, request.roleIds()); }

  @GetMapping("/saved-filters/{resource}")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId)")
  public List<Map<String, Object>> savedFilters(@PathVariable UUID tenantId, @PathVariable String resource,
      @AuthenticationPrincipal CurrentUser currentUser) {
    UserSavedFilterPreference value = preferences.get(currentUser).savedFilters().get(resource);
    if (value == null || value.savedFilters() == null) return List.of();
    return value.savedFilters().stream().map(this::toShellFilter).toList();
  }

  @PutMapping("/saved-filters/{resource}")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId)")
  public List<Map<String, Object>> saveFilters(@PathVariable UUID tenantId, @PathVariable String resource,
      @AuthenticationPrincipal CurrentUser currentUser, @RequestBody List<JsonNode> request) {
    List<UserSavedFilter> filters = request.stream().map(this::toCoreFilter).toList();
    UserPreferencesRequest patch = new UserPreferencesRequest(null, null, null, null, null,
        null, null, null, Map.of(resource, new UserSavedFilterPreference(null, filters)));
    preferences.update(currentUser, patch);
    return filters.stream().map(this::toShellFilter).toList();
  }

  @GetMapping("/alerts")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.request')")
  public List<SavedFilterAlertView> alerts(@PathVariable UUID tenantId,
      @AuthenticationPrincipal CurrentUser currentUser) { return alerts.listMine(currentUser); }

  @PostMapping("/alerts") @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.request')")
  public SavedFilterAlertView createAlert(@PathVariable UUID tenantId,
      @AuthenticationPrincipal CurrentUser currentUser, @RequestBody JsonNode request) {
    return alerts.create(currentUser, createAlertRequest(request));
  }

  @PutMapping("/alerts/{id}")
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.request')")
  public SavedFilterAlertView updateAlert(@PathVariable UUID tenantId, @PathVariable UUID id,
      @AuthenticationPrincipal CurrentUser currentUser, @RequestBody JsonNode request) {
    return alerts.update(currentUser, id, new SavedFilterAlertUpdateRequest(
        booleanOrNull(request, "enabled"), text(request, "schedule", "cron"), alertFilters(request),
        booleanOrNull(request, "channelEmail"), booleanOrNull(request, "channelMessage"),
        null, null, null));
  }

  @DeleteMapping("/alerts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.request')")
  public void deleteAlert(@PathVariable UUID tenantId, @PathVariable UUID id,
      @AuthenticationPrincipal CurrentUser currentUser) { alerts.delete(currentUser, id); }

  @PostMapping("/alerts/{id}/run-now") @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("@tenantAuth.validateTenantAccess(#tenantId) and @permissionService.hasPermission('alerts.request')")
  public void runAlert(@PathVariable UUID tenantId, @PathVariable UUID id,
      @AuthenticationPrincipal CurrentUser currentUser) {
    alerts.assertCanRunNow(currentUser, id); alertRunner.runNow(id);
  }

  private SavedFilterAlertCreateRequest createAlertRequest(JsonNode request) {
    String entity = text(request, "resource", "entityKey");
    String name = text(request, "name");
    String field = text(request, "field");
    return new SavedFilterAlertCreateRequest(entity, name, name, alertFilters(request),
        field == null ? List.of() : List.of(field), List.of(), text(request, "schedule", "cron"),
        request.path("channelEmail").asBoolean(false), request.path("channelMessage").asBoolean(true),
        List.of(), List.of(), List.of());
  }

  private String alertFilters(JsonNode request) {
    if (request.hasNonNull("filters")) return request.get("filters").isTextual()
        ? request.get("filters").asText() : request.get("filters").toString();
    String field = text(request, "field");
    if (field == null) return "{}";
    return mapper.valueToTree(Map.of(field, request.path("value").asText())).toString();
  }

  private UserSavedFilter toCoreFilter(JsonNode node) {
    String id = text(node, "id");
    String name = text(node, "name", "description");
    String field = text(node, "field");
    Map<String, Object> values = new LinkedHashMap<>();
    if (node.has("filters") && node.get("filters").isObject()) {
      node.get("filters").fields().forEachRemaining(entry ->
          values.put(entry.getKey(), mapper.convertValue(entry.getValue(), Object.class)));
    }
    if (field != null) values.put(field, mapper.convertValue(node.get("value"), Object.class));
    return new UserSavedFilter(id, name, values, Map.of(), null, null, null);
  }

  private Map<String, Object> toShellFilter(UserSavedFilter filter) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", filter.id()); result.put("name", filter.description());
    if (filter.filters() != null && !filter.filters().isEmpty()) {
      result.put("filters", filter.filters());
      var entry = filter.filters().entrySet().iterator().next();
      result.put("field", entry.getKey()); result.put("value", entry.getValue());
    }
    return result;
  }

  private static String text(JsonNode node, String... names) {
    for (String name : names) if (node.hasNonNull(name) && !node.get(name).asText().isBlank()) return node.get(name).asText();
    return null;
  }
  private static Boolean booleanOrNull(JsonNode node, String name) {
    return node.has(name) && !node.get(name).isNull() ? node.get(name).asBoolean() : null;
  }
}
