package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.service.permission.RolePermissionService;
import es.idynamicsax.idax.service.permission.dto.PermissionInfo;
import es.idynamicsax.idax.service.permission.dto.RoleRequest;
import es.idynamicsax.idax.service.permission.dto.RoleSummary;
import es.idynamicsax.idax.service.permission.dto.RoleUserInfo;
import io.github.tonisoler.idaxshell.extensions.ExtensionRegistry;
import io.github.tonisoler.idaxshell.extensions.RoleTemplate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Keeps, in a workspace, one Core role per role template the installed modules declare (see
 * {@link RoleTemplate}), and gives the default ones to members. Composed only from Core's public
 * RolePermissionService, always in the caller's tenant context (Core checks it):
 *
 * <ul>
 *   <li>A missing template role is created, enabled, with the template's permissions.</li>
 *   <li>An existing one only ever gains permissions: a permission a newer module version adds is
 *       added; one an administrator added is kept; nothing is removed.</li>
 *   <li>A permission the Core catalog does not know yet (its module has not registered it) is
 *       reported as unavailable and added on a later reconciliation, never an error.</li>
 *   <li>An administrator may rename or disable a template role. A disabled one is never given to
 *       anyone. It cannot be deleted, since it would come back on the next reconciliation.</li>
 * </ul>
 */
@Service
public class RoleTemplateService {
  public static final String SOURCE_TYPE = "EXTENSION_TEMPLATE";
  private static final Logger log = LoggerFactory.getLogger(RoleTemplateService.class);
  private final ExtensionRegistry registry;
  private final RolePermissionService roles;

  public RoleTemplateService(ExtensionRegistry registry, RolePermissionService roles) {
    this.registry = registry; this.roles = roles;
  }

  /** One template as it stands in a workspace; roleId is null until the role exists. */
  public record TemplateState(String extensionId, String extensionName, String key, String name, String description,
      boolean defaultForNewMembers, List<String> permissions, UUID roleId, boolean roleEnabled,
      List<String> unavailablePermissions) {}

  public record ApplyResult(List<TemplateState> templates, int membersAssigned) {}

  public boolean hasTemplates() { return !registry.roleTemplates().isEmpty(); }

  /** Reads only: what each template looks like in this workspace. */
  public List<TemplateState> describe(UUID tenantId) {
    if (!hasTemplates()) return List.of();
    Set<String> catalog = catalog();
    Map<String, RoleSummary> existing = templateRoles(tenantId);
    return registry.roleTemplates().stream().map(template -> {
      RoleSummary role = existing.get(template.sourceKey());
      return state(template, role, unavailable(template, catalog));
    }).toList();
  }

  /** Creates the missing template roles and adds missing permissions to the existing ones. */
  public List<TemplateState> reconcile(UUID tenantId) {
    if (!hasTemplates()) return List.of();
    Set<String> catalog = catalog();
    Map<String, RoleSummary> existing = templateRoles(tenantId);
    List<TemplateState> states = new ArrayList<>();
    for (RoleTemplate template : registry.roleTemplates()) {
      List<String> unavailable = unavailable(template, catalog);
      Set<String> wanted = new LinkedHashSet<>(template.permissions());
      wanted.removeAll(unavailable);
      if (!unavailable.isEmpty())
        log.warn("Role template {} in tenant {}: permissions not in the Core catalog yet, added later: {}",
            template.sourceKey(), tenantId, unavailable);
      RoleSummary role = existing.get(template.sourceKey());
      if (role == null) {
        role = roles.create(tenantId, new RoleRequest(template.roleKey(), template.name(), template.description(),
            Boolean.TRUE, SOURCE_TYPE, template.sourceKey(), wanted));
        log.info("Role template {} created in tenant {} as role {}", template.sourceKey(), tenantId, role.id());
      } else {
        Set<String> current = roles.rolePermissions(tenantId, role.id());
        if (!current.containsAll(wanted)) {
          Set<String> merged = new TreeSet<>(current);
          merged.addAll(wanted);
          roles.replacePermissions(tenantId, role.id(), merged);
        }
      }
      states.add(state(template, role, unavailable));
    }
    return states;
  }

  /** Gives a member every enabled default template role, keeping the roles they already have. */
  public void assignDefaults(UUID tenantId, UUID userId) {
    Set<UUID> defaults = defaultRoleIds(reconcile(tenantId));
    if (defaults.isEmpty()) return;
    roles.listUsers(tenantId).stream().filter(user -> user.userId().equals(userId)).findFirst()
        .ifPresent(user -> addRoles(tenantId, user, defaults));
  }

  /** Reconciles, and optionally gives the default roles to every current member who lacks one. */
  public ApplyResult apply(UUID tenantId, boolean assignToExistingMembers) {
    List<TemplateState> states = reconcile(tenantId);
    int assigned = 0;
    if (assignToExistingMembers) {
      Set<UUID> defaults = defaultRoleIds(states);
      if (!defaults.isEmpty())
        for (RoleUserInfo user : roles.listUsers(tenantId)) if (addRoles(tenantId, user, defaults)) assigned++;
    }
    return new ApplyResult(describe(tenantId), assigned);
  }

  /** True when this role implements a module's template. */
  public boolean isTemplateRole(UUID tenantId, UUID roleId) {
    return roles.listRoles(tenantId).stream()
        .anyMatch(role -> role.id().equals(roleId) && SOURCE_TYPE.equals(role.sourceType()) && role.sourceKey() != null);
  }

  private boolean addRoles(UUID tenantId, RoleUserInfo user, Set<UUID> defaults) {
    Set<UUID> current = user.roleIds() == null ? Set.of() : user.roleIds();
    if (current.containsAll(defaults)) return false;
    Set<UUID> merged = new LinkedHashSet<>(current);
    merged.addAll(defaults);
    roles.replaceUserRoles(tenantId, user.userId(), merged);
    return true;
  }

  private static Set<UUID> defaultRoleIds(List<TemplateState> states) {
    return states.stream().filter(s -> s.defaultForNewMembers() && s.roleId() != null && s.roleEnabled())
        .map(TemplateState::roleId).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Map<String, RoleSummary> templateRoles(UUID tenantId) {
    return roles.listRoles(tenantId).stream()
        .filter(role -> SOURCE_TYPE.equals(role.sourceType()) && role.sourceKey() != null)
        .collect(Collectors.toMap(RoleSummary::sourceKey, Function.identity(), (first, second) -> first));
  }

  private Set<String> catalog() {
    return roles.permissionCatalog().stream().map(PermissionInfo::code).collect(Collectors.toSet());
  }

  private static List<String> unavailable(RoleTemplate template, Set<String> catalog) {
    return template.permissions().stream().filter(code -> !catalog.contains(code)).toList();
  }

  private static TemplateState state(RoleTemplate template, RoleSummary role, List<String> unavailable) {
    return new TemplateState(template.extensionId(), template.extensionName(), template.key(), template.name(),
        template.description(), template.defaultForNewMembers(), template.permissions(),
        role == null ? null : role.id(), role != null && role.enabled(), unavailable);
  }
}
