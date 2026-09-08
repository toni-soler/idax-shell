package io.github.tonisoler.idaxshell;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tonisoler.idaxshell.admin.AdministrationController;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdministrationContractTest {
  @Test
  void administrationIsBackedByConcreteCoreCompositionWithoutTheFormer501Adapter() {
    assertNotNull(AdministrationController.class);
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("io.github.tonisoler.idaxshell.admin.UnavailableCoreAdministrationAdapter"));
  }

  @Test
  void publicAdaptersComposeCoreIdentityServicesAndPermissions() throws Exception {
    String authentication = Files.readString(Path.of("src/main/java/io/github/tonisoler/idaxshell/auth/LocalAuthenticationService.java"));
    String administration = Files.readString(Path.of("src/main/java/io/github/tonisoler/idaxshell/admin/AdministrationController.java"));
    String security = Files.readString(Path.of("src/main/java/io/github/tonisoler/idaxshell/config/SecurityConfiguration.java"));

    assertTrue(authentication.contains("LocalAuthService"));
    assertTrue(authentication.contains("CurrentUser"));
    assertFalse(authentication.contains("JdbcTemplate"));
    assertFalse(authentication.contains("List.of(\"owner\")"));
    assertTrue(administration.contains("TenantUserService"));
    assertTrue(administration.contains("RolePermissionService"));
    assertTrue(administration.contains("UserPreferencesService"));
    assertTrue(administration.contains("SavedFilterAlertService"));
    assertTrue(administration.contains("system.users.read"));
    assertTrue(administration.contains("system.roles.manage"));
    assertTrue(security.contains("JwtAuthFilter"));
    assertTrue(security.contains("TenantContextFilter"));
    assertFalse(security.contains("oauth2ResourceServer"));
  }
}
