package io.github.tonisoler.idaxshell.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tonisoler.idaxshell.config.ShellProperties;
import java.io.IOException;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/shell/v1/extensions")
public class ExtensionController {
  private final ObjectMapper mapper; private final ShellProperties properties;
  public ExtensionController(ObjectMapper mapper,ShellProperties properties){this.mapper=mapper;this.properties=properties;}
  @GetMapping public JsonNode manifest() throws IOException {
    JsonNode root=mapper.readTree(properties.extensionManifest().getInputStream());
    if(root.path("schemaVersion").asInt()!=1 || !root.path("extensions").isArray()) throw new IllegalStateException("Unsupported extension manifest");
    return root;
  }
}

