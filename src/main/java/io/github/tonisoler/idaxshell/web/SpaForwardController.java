package io.github.tonisoler.idaxshell.web;

import io.github.tonisoler.idaxshell.extensions.ExtensionRegistry;
import java.util.LinkedHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/** Maps only manifest-declared SPA prefixes; static resources and APIs retain their handlers. */
@Configuration
public class SpaForwardController {
    @Bean SimpleUrlHandlerMapping extensionSpaMapping(ExtensionRegistry extensions) {
        HttpRequestHandler handler=(request,response)->{
            if(!extensions.isSpaRequest(request)){response.sendError(404);return;}
            request.getRequestDispatcher("/index.html").forward(request,response);
        };
        var mappings=new LinkedHashMap<String,Object>();
        for(String route:extensions.routes()){mappings.put(route,handler);mappings.put(route+"/**",handler);}
        return new SimpleUrlHandlerMapping(mappings,1);
    }
}
