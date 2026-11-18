package io.github.tonisoler.idaxshell.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import es.idynamicsax.idax.service.permission.RolePermissionService;
import es.idynamicsax.idax.service.permission.dto.PermissionInfo;
import es.idynamicsax.idax.service.permission.dto.RoleRequest;
import es.idynamicsax.idax.service.permission.dto.RoleSummary;
import es.idynamicsax.idax.service.permission.dto.RoleUserInfo;
import io.github.tonisoler.idaxshell.extensions.ExtensionRegistry;
import io.github.tonisoler.idaxshell.extensions.RoleTemplate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A module's role templates in one workspace: created when missing, only ever gaining permissions,
 * tolerant of permissions the Core catalog does not know yet, and given to members by default
 * without taking away the roles they have.
 */
class RoleTemplateServiceTest {
  static final UUID TENANT = UUID.randomUUID();
  final RolePermissionService roles = mock(RolePermissionService.class);
  final RoleTemplateService service;

  RoleTemplateServiceTest() {
    // The manifest parsing itself is RoleTemplateManifestTest's subject.
    var registry = mock(ExtensionRegistry.class);
    when(registry.roleTemplates()).thenReturn(List.of(
        new RoleTemplate("garden", "Garden", "member", "Garden member", "", List.of("garden.beds.read", "garden.water"), true),
        new RoleTemplate("garden", "Garden", "steward", "Garden steward", "", List.of("garden.beds.manage"), false)));
    service = new RoleTemplateService(registry, roles);
    when(roles.permissionCatalog()).thenReturn(List.of(permission("garden.beds.read"), permission("garden.water"), permission("garden.beds.manage")));
  }

  static PermissionInfo permission(String code) { return new PermissionInfo(code, "garden", "beds", null, "read", null, null, "MODULE"); }
  static RoleSummary role(UUID id, String sourceKey, boolean enabled) {
    return new RoleSummary(id, "garden_x", "Garden x", null, enabled, false, RoleTemplateService.SOURCE_TYPE, sourceKey, 1, 0);
  }
  static RoleUserInfo member(UUID userId, Set<UUID> roleIds) { return new RoleUserInfo(userId, "s", "e@x", "E", "user", roleIds); }

  @Test void missingTemplateRolesAreCreatedWithTheirSourceAndPermissions() {
    when(roles.listRoles(TENANT)).thenReturn(List.of());
    when(roles.create(eq(TENANT), any())).thenAnswer(call -> role(UUID.randomUUID(), ((RoleRequest) call.getArgument(1)).sourceKey(), true));

    var states = service.reconcile(TENANT);

    var created = ArgumentCaptor.forClass(RoleRequest.class);
    verify(roles, times(2)).create(eq(TENANT), created.capture());
    var member = created.getAllValues().get(0);
    assertEquals("garden_member", member.key());
    assertEquals("Garden member", member.name());
    assertEquals(Boolean.TRUE, member.enabled());
    assertEquals("EXTENSION_TEMPLATE", member.sourceType());
    assertEquals("garden:member", member.sourceKey());
    assertEquals(Set.of("garden.beds.read", "garden.water"), member.permissions());
    assertTrue(states.stream().allMatch(state -> state.roleId() != null && state.unavailablePermissions().isEmpty()));
  }

  @Test void anExistingTemplateRoleOnlyGainsPermissionsAndKeepsAnAdministratorsAdditions() {
    UUID memberRole = UUID.randomUUID(), stewardRole = UUID.randomUUID();
    when(roles.listRoles(TENANT)).thenReturn(List.of(role(memberRole, "garden:member", true), role(stewardRole, "garden:steward", true)));
    when(roles.rolePermissions(TENANT, memberRole)).thenReturn(Set.of("garden.beds.read", "garden.extra"));
    when(roles.rolePermissions(TENANT, stewardRole)).thenReturn(Set.of("garden.beds.manage"));

    service.reconcile(TENANT);

    verify(roles).replacePermissions(TENANT, memberRole, Set.of("garden.beds.read", "garden.extra", "garden.water"));
    verify(roles, never()).replacePermissions(eq(TENANT), eq(stewardRole), any());
    verify(roles, never()).create(any(), any());
  }

  @Test void aPermissionTheCatalogDoesNotKnowYetIsReportedNotSent() {
    when(roles.permissionCatalog()).thenReturn(List.of(permission("garden.beds.read"), permission("garden.beds.manage")));
    when(roles.listRoles(TENANT)).thenReturn(List.of());
    when(roles.create(eq(TENANT), any())).thenAnswer(call -> role(UUID.randomUUID(), ((RoleRequest) call.getArgument(1)).sourceKey(), true));

    var member = service.reconcile(TENANT).get(0);

    var created = ArgumentCaptor.forClass(RoleRequest.class);
    verify(roles, times(2)).create(eq(TENANT), created.capture());
    assertEquals(Set.of("garden.beds.read"), created.getAllValues().get(0).permissions(), "Core rejects unknown codes");
    assertEquals(List.of("garden.water"), member.unavailablePermissions());
  }

  @Test void aNewMemberGetsTheDefaultRolesAndKeepsTheirOwn() {
    UUID memberRole = UUID.randomUUID(), stewardRole = UUID.randomUUID(), ownRole = UUID.randomUUID(), user = UUID.randomUUID();
    when(roles.listRoles(TENANT)).thenReturn(List.of(role(memberRole, "garden:member", true), role(stewardRole, "garden:steward", true)));
    when(roles.rolePermissions(eq(TENANT), any())).thenReturn(Set.of("garden.beds.read", "garden.water", "garden.beds.manage"));
    when(roles.listUsers(TENANT)).thenReturn(List.of(member(user, Set.of(ownRole))));

    service.assignDefaults(TENANT, user);

    verify(roles).replaceUserRoles(TENANT, user, Set.of(ownRole, memberRole));
  }

  @Test void aDisabledTemplateRoleIsNeverGivenToAnyone() {
    UUID memberRole = UUID.randomUUID(), user = UUID.randomUUID();
    when(roles.listRoles(TENANT)).thenReturn(List.of(role(memberRole, "garden:member", false), role(UUID.randomUUID(), "garden:steward", true)));
    when(roles.rolePermissions(eq(TENANT), any())).thenReturn(Set.of("garden.beds.read", "garden.water", "garden.beds.manage"));
    when(roles.listUsers(TENANT)).thenReturn(List.of(member(user, Set.of())));

    service.assignDefaults(TENANT, user);
    var result = service.apply(TENANT, true);

    verify(roles, never()).replaceUserRoles(any(), any(), any());
    assertEquals(0, result.membersAssigned());
  }

  @Test void applyGivesTheDefaultRolesOnlyToMembersWhoLackThem() {
    UUID memberRole = UUID.randomUUID(), has = UUID.randomUUID(), lacks = UUID.randomUUID();
    when(roles.listRoles(TENANT)).thenReturn(List.of(role(memberRole, "garden:member", true), role(UUID.randomUUID(), "garden:steward", true)));
    when(roles.rolePermissions(eq(TENANT), any())).thenReturn(Set.of("garden.beds.read", "garden.water", "garden.beds.manage"));
    when(roles.listUsers(TENANT)).thenReturn(List.of(member(has, Set.of(memberRole)), member(lacks, Set.of())));

    assertEquals(0, service.apply(TENANT, false).membersAssigned(), "reconciling alone assigns nobody");
    verify(roles, never()).replaceUserRoles(any(), any(), any());

    var result = service.apply(TENANT, true);

    assertEquals(1, result.membersAssigned());
    verify(roles).replaceUserRoles(TENANT, lacks, Set.of(memberRole));
    verify(roles, never()).replaceUserRoles(eq(TENANT), eq(has), any());
  }

  @Test void updatingATemplateRoleKeepsItsSourceAndNoOtherRoleCanClaimOne() {
    UUID template = UUID.randomUUID(), plain = UUID.randomUUID();
    when(roles.listRoles(TENANT)).thenReturn(List.of(role(template, "garden:member", true),
        new RoleSummary(plain, "own", "Own", null, true, false, "IDAX", null, 0, 0)));

    var renamed = service.guard(TENANT, template, new RoleRequest(null, "Gardeners", null, false, null, null, null));
    assertEquals("Gardeners", renamed.name());
    assertEquals(Boolean.FALSE, renamed.enabled());
    assertEquals("EXTENSION_TEMPLATE", renamed.sourceType());
    assertEquals("garden:member", renamed.sourceKey(), "Core would clear a missing sourceKey");

    var own = new RoleRequest("own", "Own", null, true, null, null, null);
    assertSame(own, service.guard(TENANT, plain, own));
    assertThrows(IllegalArgumentException.class, () -> service.guard(TENANT, plain,
        new RoleRequest("own", "Own", null, true, "extension_template", "garden:member", null)));
    assertThrows(IllegalArgumentException.class, () -> service.guard(TENANT, null,
        new RoleRequest("fake", "Fake", null, true, "EXTENSION_TEMPLATE", "garden:steward", null)));
  }

  @Test void onlyRolesThatImplementATemplateAreTemplateRoles() {
    UUID template = UUID.randomUUID(), plain = UUID.randomUUID();
    when(roles.listRoles(TENANT)).thenReturn(List.of(role(template, "garden:member", true),
        new RoleSummary(plain, "own", "Own", null, true, false, "IDAX", null, 0, 0)));
    assertTrue(service.isTemplateRole(TENANT, template));
    assertFalse(service.isTemplateRole(TENANT, plain));
  }
}
