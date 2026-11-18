package io.github.tonisoler.idaxshell.extensions;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Role templates in the extension manifest: parsed in order, and a malformed one fails closed. */
class RoleTemplateManifestTest {
  static ExtensionRegistry registry(String templates) throws Exception {
    return new ExtensionRegistry(new ObjectMapper().readTree(
        "{\"schemaVersion\":1,\"extensions\":[{\"id\":\"garden\",\"name\":\"Garden\",\"route\":\"/garden\""
            + (templates == null ? "" : ",\"roleTemplates\":" + templates) + "},{\"id\":\"plain\",\"route\":\"/plain\"}]}"));
  }

  @Test void templatesAreOptionalAndParsedInManifestOrder() throws Exception {
    assertEquals(List.of(), registry(null).roleTemplates());
    var templates = registry("""
        [{"key":"member","name":"Garden member","description":"Grows things","permissions":["garden.beds.read","garden.beds.read","GARDEN_WATER"],"defaultForNewMembers":true},
         {"key":"steward","name":"Garden steward","permissions":["garden.beds.manage"]}]""").roleTemplates();
    assertEquals(2, templates.size());
    var member = templates.get(0);
    assertEquals("garden", member.extensionId());
    assertEquals("Garden", member.extensionName());
    assertEquals("garden:member", member.sourceKey());
    assertEquals("garden_member", member.roleKey());
    assertEquals(List.of("garden.beds.read", "GARDEN_WATER"), member.permissions(), "duplicates collapse, order is kept");
    assertTrue(member.defaultForNewMembers());
    assertFalse(templates.get(1).defaultForNewMembers(), "a template is only a default when it says so");
    assertEquals("", templates.get(1).description());
  }

  @Test void malformedTemplatesFailClosed() {
    for (String bad : new String[] {
        "{}",
        "[{\"key\":\"Member\",\"name\":\"x\",\"permissions\":[\"a.b\"]}]",
        "[{\"key\":\"member\",\"name\":\" \",\"permissions\":[\"a.b\"]}]",
        "[{\"key\":\"member\",\"name\":\"x\",\"permissions\":[]}]",
        "[{\"key\":\"member\",\"name\":\"x\"}]",
        "[{\"key\":\"member\",\"name\":\"x\",\"permissions\":[\"a b\"]}]",
        "[{\"key\":\"member\",\"name\":\"x\",\"permissions\":[7]}]",
        "[{\"key\":\"member\",\"name\":\"x\",\"permissions\":[\"a.b\"],\"defaultForNewMembers\":\"yes\"}]",
        "[{\"key\":\"member\",\"name\":\"x\",\"permissions\":[\"a.b\"]},{\"key\":\"member\",\"name\":\"y\",\"permissions\":[\"a.c\"]}]"}) {
      assertThrows(IllegalArgumentException.class, () -> registry(bad), bad);
    }
  }
}
