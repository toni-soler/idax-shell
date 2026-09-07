package io.github.tonisoler.idaxshell;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tonisoler.idaxshell.admin.CoreCapabilityUnavailableException;
import io.github.tonisoler.idaxshell.admin.UnavailableCoreAdministrationAdapter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdministrationContractTest {
  @Test
  void unavailableAdapterNeverFallsBackToAnIndependentImplementation() {
    var adapter = new UnavailableCoreAdministrationAdapter();
    assertThrows(CoreCapabilityUnavailableException.class,
        () -> adapter.list("users", UUID.randomUUID()));
  }
}
