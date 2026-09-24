package io.github.tonisoler.idaxshell.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.idynamicsax.idax.security.CurrentUser;
import es.idynamicsax.idax.service.permission.PermissionService;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/shell/v1/extensions")
public class ExtensionController {
  private final ExtensionRegistry registry; private final ObjectMapper mapper; private final PermissionService permissions;
  public ExtensionController(ExtensionRegistry registry,ObjectMapper mapper,PermissionService permissions){this.registry=registry;this.mapper=mapper;this.permissions=permissions;}
  /** Manifest entries may declare an optional "requiredPermission" (e.g. Ledger's "LEDGER_READ").
   * An entry with no such field stays visible to everyone, unchanged from before this filter
   * existed. This only hides the module card for a user who can't use it - the module's own API
   * (e.g. Ledger's 403) remains the actual, authoritative enforcement; this is UX only. Routing
   * validity (ExtensionRegistry) is unrelated to per-user visibility and stays untouched. */
  @GetMapping public JsonNode manifest(@AuthenticationPrincipal CurrentUser currentUser) throws IOException {
    JsonNode root=registry.manifest();
    Set<String> effective=permissions.effectivePermissions(currentUser);
    ArrayNode visible=mapper.createArrayNode();
    root.path("extensions").forEach(ext->{
      String required=ext.path("requiredPermission").asText(null);
      if(required==null||effective.contains(required)) visible.add(ext);
    });
    return ((ObjectNode)root).set("extensions",visible);
  }
}

