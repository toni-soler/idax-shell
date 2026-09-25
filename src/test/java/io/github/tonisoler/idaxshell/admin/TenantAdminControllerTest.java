package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.repository.admin.TenantCreateRepository;
import es.idynamicsax.idax.repository.admin.TenantSearchGlobalRepository;
import es.idynamicsax.idax.repository.admin.TenantSetEnabledRepository;
import es.idynamicsax.idax.repository.admin.TenantUpdateNameRepository;
import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.service.tenant.TenantUserService;
import es.idynamicsax.idax.service.tenant.dto.TenantUserRequest;
import es.idynamicsax.idax.service.tenant.dto.TenantUserResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TenantAdminController is the only place idax-shell exposes tenant (workspace) creation -
 * previously never wired up anywhere, so a genuinely separate tenant (its own STIR marketplace,
 * its own osTRIS economic community) required hand-running SQL. Every method here is
 * @PreAuthorize("authentication.principal.superuser") - deliberately NOT the per-tenant
 * permission-code pattern AdministrationController uses, since there is no tenant context yet
 * when you're creating one.
 *
 * create() composes TenantCreateRepository + TenantUserService rather than idax-core's own
 * TenantOnboardingService.createOrBootstrapTenant, which unconditionally requires an
 * AX4-legacy-integration "idax.dataarea" table STIR's deployment never provisions - confirmed
 * live (25/09/2026): BadSqlGrammarException, relation "idax.dataarea" does not exist.
 */
class TenantAdminControllerTest {
    TenantSearchGlobalRepository search = mock(TenantSearchGlobalRepository.class);
    TenantCreateRepository create = mock(TenantCreateRepository.class);
    TenantUserService tenantUsers = mock(TenantUserService.class);
    TenantSetEnabledRepository setEnabled = mock(TenantSetEnabledRepository.class);
    TenantUpdateNameRepository updateName = mock(TenantUpdateNameRepository.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    TenantAdminController controller = new TenantAdminController(search, create, tenantUsers, setEnabled, updateName, jdbcTemplate);
    CurrentUser caller = new CurrentUser(UUID.randomUUID(), "admin@stir.es", null, true, Set.of("ROLE_SUPERUSER"));

    @Test void createComposesTenantCreationAndItsFirstLocalAdminWithoutTouchingDataArea() {
        UUID tenantId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        when(create.create("stir-pruebas", "STIR - pruebas", "active", false))
            .thenReturn(new TenantCreateRepository.TenantRow(tenantId, "stir-pruebas", "STIR - pruebas", "active", false, createdAt, createdAt));
        when(tenantUsers.create(eq(tenantId), any())).thenReturn(mock(TenantUserResponse.class));

        var result = controller.create(new TenantAdminController.TenantCreateRequest(
            "stir-pruebas", "STIR - pruebas", "admin.pruebas@stir.es", "Admin Pruebas", "hunter22"), caller);

        // must elevate to idax_admin and set the new tenant as current BEFORE creating its first
        // user - TenantUserService.create() throws SecurityException("Tenant mismatch") otherwise,
        // since it guards TenantContext.get().tenantId == the tenantId argument.
        verify(jdbcTemplate).execute("SET LOCAL ROLE idax_admin");
        verify(jdbcTemplate).execute(eq("SELECT idax_core.set_tenant(?)"), any(org.springframework.jdbc.core.PreparedStatementCallback.class));

        var captor = org.mockito.ArgumentCaptor.forClass(TenantUserRequest.class);
        verify(tenantUsers).create(eq(tenantId), captor.capture());
        var request = captor.getValue();
        assertEquals("admin.pruebas@stir.es", request.subject());
        assertEquals("admin.pruebas@stir.es", request.email());
        assertEquals("Admin Pruebas", request.displayName());
        assertEquals("local", request.authProvider());
        assertEquals("hunter22", request.password());
        assertEquals("owner", request.role());
        assertEquals(Boolean.TRUE, request.enabled());

        assertEquals(tenantId, result.id());
        assertEquals("stir-pruebas", result.code());
        assertTrue(result.enabled(), "a freshly created tenant must start enabled");
    }

    @Test void listMapsSearchResultsWithEnabledDerivedFromActiveStatus() {
        UUID tenantId = UUID.randomUUID();
        var row = new TenantSearchGlobalRepository.TenantRow(tenantId, "STIR", "STIR", "active", false, OffsetDateTime.now(), OffsetDateTime.now());
        var disabledRow = new TenantSearchGlobalRepository.TenantRow(UUID.randomUUID(), "OLD", "Old tenant", "disabled", false, OffsetDateTime.now(), OffsetDateTime.now());
        // idax_core.tenant_search_global's signature is (code, name, enabled, LIMIT, OFFSET) -
        // limit before offset. Swapping them isn't a SQL error (LIMIT 0 is valid), so it fails
        // silently with zero rows and no exception anywhere - reproduced live in production
        // (25/09/2026): the Espacios screen showed "no hay registros" for an existing tenant with
        // a clean HTTP 200 and nothing in the application logs. Stubbing the exact expected
        // argument order here means a regression fails this test, not just production.
        when(search.search(null, null, null, 500, 0)).thenReturn(new TenantSearchGlobalRepository.SearchResult(List.of(row, disabledRow), 2));

        List<TenantAdminView> views = controller.list();

        assertEquals(2, views.size());
        assertTrue(views.get(0).enabled());
        assertFalse(views.get(1).enabled());
    }

    @Test void setEnabledFailsClosedWhenTheTenantNoLongerExists() {
        UUID tenantId = UUID.randomUUID();
        when(setEnabled.setEnabled(tenantId, false)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> controller.setEnabled(tenantId, Map.of("enabled", false)));
        assertEquals(404, error.getStatusCode().value());
    }
}
