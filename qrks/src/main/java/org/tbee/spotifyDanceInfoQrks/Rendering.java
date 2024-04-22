package org.tbee.spotifyDanceInfoQrks;

import groovy.util.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.resteasy.plugins.providers.html.Renderable;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Log
@ApplicationScoped
public class Rendering {

    @Produces
    public TemplateEngine templateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(new ClassLoaderTemplateResolver());
        return templateEngine;
    }

    public ThymeleafView view(String relativePath) {
        String templatePath = String.format("templates/%s.html", relativePath);
        return new ThymeleafView(templatePath, templateEngine());
    }

    public static class ThymeleafView implements Renderable {

        private final String path;
        private final TemplateEngine templateEngine;
        private final Map<String, Object> variables = new HashMap<>();

        public ThymeleafView(String path, TemplateEngine templateEngine) {
            this.path = path;
            this.templateEngine = templateEngine;
        }

        public ThymeleafView with(String key, Object variable) {
            this.variables.put(key, variable);
            return this;
        }

        @Override
        public void render(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException, WebApplicationException {

            WebContext context = ThymeleafHelper.createContext(request, response);
            context.setVariables(variables);

            try (ServletOutputStream outputStream = response.getOutputStream();
                 OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                templateEngine.process(path, context, writer);
            }
        }
    }
}