package io.github.tonisoler.idaxshell.admin;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shell/v1/tenants/{tenantId}")
public class AdministrationController {
  private static final List<String> RESOURCES = List.of("users", "roles", "alerts");
  private final CoreAdministrationPort core;
  public AdministrationController(CoreAdministrationPort core) { this.core = core; }

  @GetMapping("/{resource:users|roles|alerts}") public List<?> list(@PathVariable UUID tenantId, @PathVariable String resource) { return core.list(resource, tenantId); }
  @PostMapping("/{resource:users|roles|alerts}") @ResponseStatus(HttpStatus.CREATED) public Object create(@PathVariable UUID tenantId, @PathVariable String resource, @RequestBody JsonNode request) { return core.create(resource, tenantId, request); }
  @PutMapping("/{resource:users|roles|alerts}/{id}") public Object update(@PathVariable UUID tenantId, @PathVariable String resource, @PathVariable UUID id, @RequestBody JsonNode request) { return core.update(resource, tenantId, id, request); }
  @DeleteMapping("/{resource:users|roles|alerts}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID tenantId, @PathVariable String resource, @PathVariable UUID id) { core.delete(resource, tenantId, id); }
  @GetMapping("/saved-filters/{resource}") public JsonNode savedFilters(@PathVariable UUID tenantId, @PathVariable String resource) { validateResource(resource); return core.savedFilters(tenantId, resource); }
  @PutMapping("/saved-filters/{resource}") public JsonNode saveFilters(@PathVariable UUID tenantId, @PathVariable String resource, @RequestBody JsonNode filters) { validateResource(resource); return core.saveFilters(tenantId, resource, filters); }

  private static void validateResource(String resource) { if (!RESOURCES.contains(resource)) throw new IllegalArgumentException("Unsupported administration resource"); }
}
