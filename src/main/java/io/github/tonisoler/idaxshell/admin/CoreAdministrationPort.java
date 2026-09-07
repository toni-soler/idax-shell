package io.github.tonisoler.idaxshell.admin;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

/** Stable Shell boundary implemented by a compatible IDAX Core Runtime adapter. */
public interface CoreAdministrationPort {
  List<?> list(String resource, UUID tenantId);
  Object create(String resource, UUID tenantId, JsonNode request);
  Object update(String resource, UUID tenantId, UUID id, JsonNode request);
  void delete(String resource, UUID tenantId, UUID id);
  JsonNode savedFilters(UUID tenantId, String resource);
  JsonNode saveFilters(UUID tenantId, String resource, JsonNode filters);
}
