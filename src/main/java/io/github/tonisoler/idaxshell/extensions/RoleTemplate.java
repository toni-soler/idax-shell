package io.github.tonisoler.idaxshell.extensions;

import java.util.List;

/**
 * A role a module asks every workspace to have, declared in the extension manifest:
 *
 * <pre>
 * "roleTemplates": [{"key": "participant", "name": "Participant", "description": "...",
 *                    "permissions": ["module.things.read", ...], "defaultForNewMembers": true}]
 * </pre>
 *
 * The Shell keeps one Core role per template in each workspace (RoleTemplateService), identified by
 * its source (EXTENSION_TEMPLATE, "extensionId:key") rather than by its name or key, so an
 * administrator may rename it. A template with defaultForNewMembers is given to every new member.
 */
public record RoleTemplate(String extensionId, String extensionName, String key, String name, String description,
                           List<String> permissions, boolean defaultForNewMembers) {
    public RoleTemplate {
        permissions = List.copyOf(permissions);
    }

    /** The stable identity of the workspace role that implements this template. */
    public String sourceKey() {
        return extensionId + ":" + key;
    }

    /** The key the role is created with; the Core normalises it to lower-case words joined by "_". */
    public String roleKey() {
        return (extensionId + "_" + key).replace('-', '_');
    }
}
