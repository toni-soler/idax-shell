package io.github.tonisoler.idaxshell.admin;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

public class UnavailableCoreAdministrationAdapter implements CoreAdministrationPort {
  private CoreCapabilityUnavailableException unavailable() { return new CoreCapabilityUnavailableException(); }
  @Override public List<?> list(String resource, UUID tenantId) { throw unavailable(); }
  @Override public Object create(String resource, UUID tenantId, JsonNode request) { throw unavailable(); }
  @Override public Object update(String resource, UUID tenantId, UUID id, JsonNode request) { throw unavailable(); }
  @Override public void delete(String resource, UUID tenantId, UUID id) { throw unavailable(); }
  @Override public JsonNode savedFilters(UUID tenantId, String resource) { throw unavailable(); }
  @Override public JsonNode saveFilters(UUID tenantId, String resource, JsonNode filters) { throw unavailable(); }
}
