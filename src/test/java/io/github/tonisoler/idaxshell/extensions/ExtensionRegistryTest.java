package io.github.tonisoler.idaxshell.extensions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ExtensionRegistryTest {
    ExtensionRegistry registry(String route)throws Exception{return new ExtensionRegistry(new ObjectMapper().readTree("{\"schemaVersion\":1,\"extensions\":[{\"id\":\"garden\",\"route\":\""+route+"\"}]}"));}
    @Test void arbitraryManifestModuleAndNestedRouteWork()throws Exception{var r=registry("/community/garden");assertTrue(r.isSpaRequest(new MockHttpServletRequest("GET","/community/garden/new")));assertTrue(r.isSpaRequest(new MockHttpServletRequest("HEAD","/community/garden")));assertFalse(r.isSpaRequest(new MockHttpServletRequest("GET","/community/gardening")));}
    @Test void moduleRouteNeverOpensApisOrWrites()throws Exception{var r=registry("/garden");assertFalse(r.isSpaRequest(new MockHttpServletRequest("POST","/garden")));assertFalse(r.isSpaRequest(new MockHttpServletRequest("GET","/api/garden")));assertFalse(r.isSpaRequest(new MockHttpServletRequest("GET","/garden/../api/private")));assertFalse(r.isSpaRequest(new MockHttpServletRequest("GET","/garden/%2e%2e/api")));}
    @Test void reservedOrInvalidManifestFailsClosed(){for(String route:new String[]{"/api","/actuator/x","//other","/garden/../api","https://example.org"})assertThrows(IllegalArgumentException.class,()->registry(route));}
}
