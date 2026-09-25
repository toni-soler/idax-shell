package io.github.tonisoler.idaxshell.admin;

import es.idynamicsax.idax.repository.admin.TenantSearchGlobalRepository;
import es.idynamicsax.idax.repository.admin.TenantSetEnabledRepository;
import es.idynamicsax.idax.repository.admin.TenantUpdateNameRepository;
import es.idynamicsax.idax.service.admin.TenantOnboardingService;
import es.idynamicsax.idax.service.admin.dto.TenantOnboardingRequest;
import es.idynamicsax.idax.service.admin.dto.TenantOnboardingResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** TenantAdminController is the only place idax-shell exposes idax-core's TenantOnboardingService
 * - previously never wired up anywhere, so creating a genuinely separate tenant (its own STIR
 * marketplace, its own osTRIS economic community) required hand-running SQL. Every method here is
 * @PreAuthorize("authentication.principal.superuser") - deliberately NOT the per-tenant
 * permission-code pattern AdministrationController uses, since there is no tenant context yet
 * when you're creating one. */
class TenantAdminControllerTest {
    TenantSearchGlobalRepository search = mock(TenantSearchGlobalRepository.class);
    TenantOnboardingService onboarding = mock(TenantOnboardingService.class);
    TenantSetEnabledRepository setEnabled = mock(TenantSetEnabledRepository.class);
    TenantUpdateNameRepository updateName = mock(TenantUpdateNameRepository.class);
    TenantAdminController controller = new TenantAdminController(search, onboarding, setEnabled, updateName);

    @Test void dataAreaIdIsAlwaysExactlyThreeUppercaseCharacters() {
        // idax-core's TenantOnboardingService.validateRequest() rejects anything else ("dataAreaId
        // must have exactly 3 characters (AX DataAreaId)") - this is an AX4-legacy-integration
        // concept irrelevant to a STIR-only tenant, so it must never be something the admin has to
        // get right by hand.
        assertEquals("STI", TenantAdminController.dataAreaIdFrom("stir-test"));
        assertEquals("PRX", TenantAdminController.dataAreaIdFrom("pr"));
        assertEquals("XXX", TenantAdminController.dataAreaIdFrom(""));
        assertEquals("A1B", TenantAdminController.dataAreaIdFrom("a1-b!c"));
    }

    @Test void createBuildsALocalAdminOnboardingRequestWithADerivedDataArea() {
        UUID tenantId = UUID.randomUUID();
        when(onboarding.createOrBootstrapTenant(any())).thenReturn(
            new TenantOnboardingResponse(true, tenantId, "PRUEBA", UUID.randomUUID(), "owner", true, "PRU"));

        var result = controller.create(new TenantAdminController.TenantCreateRequest(
            "PRUEBA", "Tenant de pruebas", "admin@pruebas.test", "Admin Pruebas", "hunter22"));

        var captor = org.mockito.ArgumentCaptor.forClass(TenantOnboardingRequest.class);
        verify(onboarding).createOrBootstrapTenant(captor.capture());
        var request = captor.getValue();
        assertEquals("PRUEBA", request.getTenantCode());
        assertEquals("Tenant de pruebas", request.getTenantName());
        assertEquals("admin@pruebas.test", request.getAdminSubject(), "local auth keys identity by email, matching the existing users editor's convention");
        assertEquals("admin@pruebas.test", request.getAdminEmail());
        assertEquals("local", request.getAdminAuthProvider());
        assertEquals("hunter22", request.getAdminPassword());
        assertEquals("owner", request.getTenantRole());
        assertEquals("PRU", request.getDataAreaId());

        assertEquals(tenantId, result.id());
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
