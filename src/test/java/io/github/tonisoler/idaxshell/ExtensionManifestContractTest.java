package io.github.tonisoler.idaxshell;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class ExtensionManifestContractTest {
  @Test void bundledManifestUsesV1AndContainsAnArray() throws Exception {
    try (InputStream in=getClass().getResourceAsStream("/extensions.json")) {
      var root=new ObjectMapper().readTree(in);
      assertThat(root.path("schemaVersion").asInt()).isEqualTo(1);
      assertThat(root.path("extensions").isArray()).isTrue();
    }
  }
}

