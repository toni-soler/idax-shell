package io.github.tonisoler.idaxshell.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.service.permission.PermissionService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExtensionControllerTest {
  private static final String MANIFEST = """
      {"schemaVersion":1,"extensions":[
        {"id":"stir","name":"STIR","route":"/stir"},
        {"id":"ledger","name":"IDAX Ledger","route":"/ledger","requiredPermission":"LEDGER_READ"}
      ]}""";

  private ExtensionController controller(PermissionService permissions) throws Exception {
    var mapper = new ObjectMapper();
    var registry = new ExtensionRegistry(mapper.readTree(MANIFEST));
    return new ExtensionController(registry, mapper, permissions);
  }

  private CurrentUser user(Set<String> roles) {
    return new CurrentUser(UUID.randomUUID(), "a@stir.test", UUID.randomUUID(), false, roles);
  }

  @Test void hidesAModuleTheUserHasNoPermissionFor() throws Exception {
    var permissions = mock(PermissionService.class);
    var currentUser = user(Set.of("ROLE_MEMBER"));
    when(permissions.effectivePermissions(currentUser)).thenReturn(Set.of());
    var visible = controller(permissions).manifest(currentUser).path("extensions");
    assertThat(visible).hasSize(1);
    assertThat(visible.get(0).path("id").asText()).isEqualTo("stir");
  }

  @Test void showsAModuleOnceTheUserHoldsItsRequiredPermission() throws Exception {
    var permissions = mock(PermissionService.class);
    var currentUser = user(Set.of("ROLE_MEMBER"));
    when(permissions.effectivePermissions(currentUser)).thenReturn(Set.of("LEDGER_READ"));
    var visible = controller(permissions).manifest(currentUser).path("extensions");
    assertThat(visible).hasSize(2);
  }

  @Test void anEntryWithNoRequiredPermissionStaysVisibleToEveryone() throws Exception {
    var permissions = mock(PermissionService.class);
    var currentUser = user(Set.of());
    when(permissions.effectivePermissions(currentUser)).thenReturn(Set.of());
    var visible = controller(permissions).manifest(currentUser).path("extensions");
    assertThat(visible.get(0).path("id").asText()).isEqualTo("stir");
  }
}
