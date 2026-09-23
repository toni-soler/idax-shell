package io.github.tonisoler.idaxshell.auth;

import es.idynamicsax.idax.domain.AppUser;
import es.idynamicsax.idax.repository.AppUserRepository;
import es.idynamicsax.idax.repository.TenantRepository;
import es.idynamicsax.idax.repository.TenantUserRepository;
import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.security.TokenValidator;
import es.idynamicsax.idax.service.auth.LocalAuthService;
import es.idynamicsax.idax.service.auth.MfaForcedSetupService;
import es.idynamicsax.idax.service.auth.MfaLoginService;
import es.idynamicsax.idax.service.permission.PermissionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * A module extension's frontend (e.g. STIR) has no reliable client-side signal for "can this
 * user do X" unless Session.user.permissions actually reflects Core's own authoritative
 * effectivePermissions() for the CURRENT tenant - the JWT itself carries only role names, never
 * permission codes. This proves session()/login() populate it, instead of leaving every
 * module-defined permission (and, before this fix, every permission at all) invisible
 * client-side regardless of what the user's role actually grants.
 */
class LocalAuthenticationServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    private LocalAuthenticationService service(PermissionService permissions, AppUserRepository users) {
        return new LocalAuthenticationService(
            mock(LocalAuthService.class), mock(TokenValidator.class), users,
            mock(TenantUserRepository.class), mock(TenantRepository.class),
            mock(MfaLoginService.class), mock(MfaForcedSetupService.class), permissions);
    }

    @Test void sessionPopulatesUserPermissionsFromCoresEffectivePermissionsForTheCurrentUser() {
        var appUser = AppUser.builder().id(userId).email("a@stir.test").displayName("A").isSuperuser(false).build();
        var users = mock(AppUserRepository.class);
        when(users.findById(userId)).thenReturn(Optional.of(appUser));

        var currentUser = new CurrentUser(userId, "a@stir.test", tenantId, false, Set.of("ROLE_MEMBER"));
        var permissions = mock(PermissionService.class);
        when(permissions.effectivePermissions(currentUser)).thenReturn(Set.of("stir.moderation.manage", "stir.listings.read"));

        var session = service(permissions, users).session(currentUser);

        assertTrue(session.user().permissions().contains("stir.moderation.manage"),
            "a module-defined permission the user's role actually grants must be visible to the frontend");
        assertTrue(session.user().permissions().contains("stir.listings.read"));
        verify(permissions).effectivePermissions(currentUser);
    }

    @Test void sessionNeverInventsAPermissionCoreDidNotGrant() {
        var appUser = AppUser.builder().id(userId).email("a@stir.test").displayName("A").isSuperuser(false).build();
        var users = mock(AppUserRepository.class);
        when(users.findById(userId)).thenReturn(Optional.of(appUser));

        var currentUser = new CurrentUser(userId, "a@stir.test", tenantId, false, Set.of("ROLE_MEMBER"));
        var permissions = mock(PermissionService.class);
        when(permissions.effectivePermissions(currentUser)).thenReturn(Set.of());

        var session = service(permissions, users).session(currentUser);

        assertTrue(session.user().permissions().isEmpty());
    }
}
