package org.tbee.spotifyDanceInfoQrks;

import groovy.util.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.plugins.providers.html.Renderable;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Log
@ApplicationScoped
public class Thymeleaf {

    @Produces
    public TemplateEngine templateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(new ClassLoaderTemplateResolver());
        return templateEngine;
    }

    public ThymeleafRenderable view(String relativePath) {
        String templatePath = String.format("templates/%s.html", relativePath);
        return new ThymeleafRenderable(templatePath, templateEngine());
    }

    public static class ThymeleafRenderable implements Renderable {

        private final String path;
        private final TemplateEngine templateEngine;
        private final Map<String, Object> variables = new HashMap<>();

        public ThymeleafRenderable(String path, TemplateEngine templateEngine) {
            this.path = path;
            this.templateEngine = templateEngine;
        }

        public ThymeleafRenderable with(String key, Object variable) {
            this.variables.put(key, variable);
            return this;
        }

        @Override
        public void render(HttpServletRequest request, HttpServletResponse response) throws IOException, WebApplicationException {

            WebContext context = createContext(request, response);
            context.setVariables(variables);

            try (ServletOutputStream outputStream = response.getOutputStream();
                 OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                templateEngine.process(path, context, writer);
            }
        }

        private WebContext createContext(HttpServletRequest req, HttpServletResponse res) {
            var application = JakartaServletWebApplication.buildApplication(req.getServletContext());
            var exchange = application.buildExchange(req, res);
            return new WebContext(exchange);
        }

    }
}