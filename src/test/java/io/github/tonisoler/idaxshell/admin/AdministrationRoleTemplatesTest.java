package io.github.tonisoler.idaxshell.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.idynamicsax.idax.alerts.SavedFilterAlertRunnerService;
import es.idynamicsax.idax.alerts.SavedFilterAlertService;
import es.idynamicsax.idax.service.me.UserPreferencesService;
import es.idynamicsax.idax.service.permission.RolePermissionService;
import es.idynamicsax.idax.service.tenant.TenantUserService;
import es.idynamicsax.idax.service.tenant.dto.TenantUserRequest;
import es.idynamicsax.idax.service.tenant.dto.TenantUserResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Members join with the default template roles; a template role is disabled, never deleted. */
class AdministrationRoleTemplatesTest {
  final UUID tenant = UUID.randomUUID();
  final TenantUserService users = mock(TenantUserService.class);
  final RolePermissionService roles = mock(RolePermissionService.class);
  final RoleTemplateService templates = mock(RoleTemplateService.class);
  final AdministrationController controller = new AdministrationController(users, roles, mock(UserPreferencesService.class),
      mock(SavedFilterAlertService.class), mock(SavedFilterAlertRunnerService.class), new ObjectMapper(), templates);

  @Test void aNewMemberJoinsWithTheDefaultTemplateRoles() {
    UUID userId = UUID.randomUUID();
    var created = new TenantUserResponse(userId, "ana@x", "ana@x", "Ana", "local", "user", true, false);
    var request = new TenantUserRequest("ana@x", "ana@x", "Ana", "local", "secret", "user", true, false);
    when(users.create(tenant, request)).thenReturn(created);

    assertSame(created, controller.createUser(tenant, request));

    var order = inOrder(users, templates);
    order.verify(users).create(tenant, request);
    order.verify(templates).assignDefaults(tenant, userId);
  }

  @Test void aTemplateRoleCannotBeDeletedButAnyOtherRoleCan() {
    UUID template = UUID.randomUUID(), plain = UUID.randomUUID();
    when(templates.isTemplateRole(tenant, template)).thenReturn(true);

    var refused = assertThrows(ResponseStatusException.class, () -> controller.deleteRole(tenant, template));
    assertEquals(409, refused.getStatusCode().value());
    verify(roles, never()).delete(any(), any());

    controller.deleteRole(tenant, plain);
    verify(roles).delete(tenant, plain);
  }

  @Test void roleWritesGoThroughTheTemplateGuard() {
    UUID roleId = UUID.randomUUID();
    var request = new es.idynamicsax.idax.service.permission.dto.RoleRequest("k", "N", null, true, null, null, null);
    var guarded = new es.idynamicsax.idax.service.permission.dto.RoleRequest("k", "N", null, true, "EXTENSION_TEMPLATE", "garden:member", null);
    when(templates.guard(tenant, roleId, request)).thenReturn(guarded);
    controller.updateRole(tenant, roleId, request);
    verify(roles).update(tenant, roleId, guarded);

    var claim = new es.idynamicsax.idax.service.permission.dto.RoleRequest("f", "F", null, true, "EXTENSION_TEMPLATE", "garden:x", null);
    when(templates.guard(tenant, null, claim)).thenThrow(new IllegalArgumentException("no"));
    var refused = assertThrows(ResponseStatusException.class, () -> controller.createRole(tenant, claim));
    assertEquals(400, refused.getStatusCode().value());
    verify(roles, never()).create(any(), any());
  }

  @Test void applyingReconcilesAndOnlyAssignsExistingMembersWhenAsked() {
    when(templates.apply(any(), anyBoolean())).thenReturn(new RoleTemplateService.ApplyResult(List.of(), 0));
    controller.applyRoleTemplates(tenant, null);
    verify(templates).apply(tenant, false);
    controller.applyRoleTemplates(tenant, new AdministrationController.ApplyTemplatesRequest(true));
    verify(templates).apply(tenant, true);
  }
}
