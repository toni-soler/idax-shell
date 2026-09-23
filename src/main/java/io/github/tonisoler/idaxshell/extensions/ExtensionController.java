package io.github.tonisoler.idaxshell.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tonisoler.idaxshell.config.ShellProperties;
import java.io.IOException;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/shell/v1/extensions")
public class ExtensionController {
  private final ExtensionRegistry registry;
  public ExtensionController(ExtensionRegistry registry){this.registry=registry;}
  @GetMapping public JsonNode manifest() throws IOException {
    return registry.manifest();
  }
}

