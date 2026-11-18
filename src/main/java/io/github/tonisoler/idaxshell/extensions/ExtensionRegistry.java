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
    private final List<RoleTemplate> roleTemplates;
    @org.springframework.beans.factory.annotation.Autowired
    public ExtensionRegistry(ObjectMapper mapper, ShellProperties properties) throws IOException {
        this(mapper.readTree(properties.extensionManifest().getInputStream()));
    }
    ExtensionRegistry(JsonNode manifest) {
        if(manifest.path("schemaVersion").asInt()!=1 || !manifest.path("extensions").isArray()) throw new IllegalArgumentException("Unsupported extension manifest");
        var ids=new HashSet<String>(); var paths=new ArrayList<String>(); var templates=new ArrayList<RoleTemplate>();
        for(var extension:manifest.path("extensions")) {
            String id=extension.path("id").asText(), route=extension.path("route").asText();
            if(!id.matches("[a-z][a-z0-9-]*") || !ids.add(id) || !route.matches("(/[a-z][a-z0-9-]*)+") || RESERVED.contains(route.split("/")[1])) throw new IllegalArgumentException("Invalid extension id/route");
            if(paths.stream().anyMatch(p->route.equals(p)||route.startsWith(p+"/")||p.startsWith(route+"/")))throw new IllegalArgumentException("Overlapping extension routes");
            paths.add(route);
            templates.addAll(roleTemplates(id, extension));
        }
        this.manifest=manifest.deepCopy();this.routes=List.copyOf(paths);this.roleTemplates=List.copyOf(templates);
    }
    public JsonNode manifest(){return manifest.deepCopy();}
    public List<String> routes(){return routes;}
    /** Every role template every extension declares, in manifest order. */
    public List<RoleTemplate> roleTemplates(){return roleTemplates;}

    /** Fails closed: a malformed template would otherwise grant the wrong permissions in every workspace. */
    private static List<RoleTemplate> roleTemplates(String extensionId, JsonNode extension) {
        JsonNode declared=extension.path("roleTemplates");
        if(declared.isMissingNode())return List.of();
        if(!declared.isArray())throw new IllegalArgumentException("Invalid role templates in extension "+extensionId);
        var keys=new HashSet<String>(); var result=new ArrayList<RoleTemplate>();
        for(var template:declared) {
            String key=template.path("key").asText(), name=template.path("name").asText().trim();
            JsonNode permissions=template.path("permissions"), byDefault=template.path("defaultForNewMembers");
            if(!key.matches("[a-z][a-z0-9-]{0,39}") || !keys.add(key) || name.isEmpty() || name.length()>160
                || template.path("description").asText().length()>500 || !permissions.isArray() || permissions.isEmpty()
                || !(byDefault.isMissingNode() || byDefault.isBoolean()))
                throw new IllegalArgumentException("Invalid role template "+extensionId+":"+key);
            var codes=new LinkedHashSet<String>();
            for(var permission:permissions) {
                if(!permission.isTextual() || !permission.asText().matches("[A-Za-z][A-Za-z0-9_.:-]{0,159}"))
                    throw new IllegalArgumentException("Invalid permission in role template "+extensionId+":"+key);
                codes.add(permission.asText());
            }
            result.add(new RoleTemplate(extensionId, extension.path("name").asText(extensionId), key, name,
                template.path("description").asText(""), List.copyOf(codes), byDefault.asBoolean(false)));
        }
        return result;
    }
    public boolean isSpaRequest(HttpServletRequest request) {
        if(!Set.of("GET","HEAD").contains(request.getMethod()))return false;
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(path.contains("%")||path.contains("..")||path.contains("\\")||path.contains("//"))return false;
        return routes.stream().anyMatch(route->path.equals(route)||path.startsWith(route+"/"));
    }
}
