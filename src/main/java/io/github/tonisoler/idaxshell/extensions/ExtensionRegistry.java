package io.github.tonisoler.idaxshell.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tonisoler.idaxshell.config.ShellProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Component;

/** Closed manifest routes, never a blanket public SPA/API wildcard. */
@Component
public class ExtensionRegistry {
    private static final Set<String> RESERVED=Set.of("api","actuator","assets","extensions","generated","swagger-ui","v3","users","roles","alerts","modules","error");
    private final JsonNode manifest;
    private final List<String> routes;
    @org.springframework.beans.factory.annotation.Autowired
    public ExtensionRegistry(ObjectMapper mapper, ShellProperties properties) throws IOException {
        this(mapper.readTree(properties.extensionManifest().getInputStream()));
    }
    ExtensionRegistry(JsonNode manifest) {
        if(manifest.path("schemaVersion").asInt()!=1 || !manifest.path("extensions").isArray()) throw new IllegalArgumentException("Unsupported extension manifest");
        var ids=new HashSet<String>(); var paths=new ArrayList<String>();
        for(var extension:manifest.path("extensions")) {
            String id=extension.path("id").asText(), route=extension.path("route").asText();
            if(!id.matches("[a-z][a-z0-9-]*") || !ids.add(id) || !route.matches("(/[a-z][a-z0-9-]*)+") || RESERVED.contains(route.split("/")[1])) throw new IllegalArgumentException("Invalid extension id/route");
            if(paths.stream().anyMatch(p->route.equals(p)||route.startsWith(p+"/")||p.startsWith(route+"/")))throw new IllegalArgumentException("Overlapping extension routes");
            paths.add(route);
        }
        this.manifest=manifest.deepCopy();this.routes=List.copyOf(paths);
    }
    public JsonNode manifest(){return manifest.deepCopy();}
    public List<String> routes(){return routes;}
    public boolean isSpaRequest(HttpServletRequest request) {
        if(!Set.of("GET","HEAD").contains(request.getMethod()))return false;
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(path.contains("%")||path.contains("..")||path.contains("\\")||path.contains("//"))return false;
        return routes.stream().anyMatch(route->path.equals(route)||path.startsWith(route+"/"));
    }
}
