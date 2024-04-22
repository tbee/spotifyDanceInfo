package org.tbee.spotifyDanceInfoQrks;

import groovy.util.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.plugins.providers.html.Renderable;

import java.time.LocalDateTime;
import java.util.Optional;

@Log
@Path("")
@Produces(MediaType.TEXT_HTML)
public class HtmlResource {

    @Inject
    Thymeleaf thymeleaf;

    @GET
    @Path("")
    public Renderable query(@QueryParam("name") @DefaultValue("Buddy") String name) {
        return thymeleaf.view("index")
                .with("name", getOr(name, "Friend"))
                .with("now", LocalDateTime.now());
    }

    @GET
    @Path("{path}") // @Path("{path: .*}")
    public Renderable path(@PathParam("path") String path) {
        return thymeleaf.view("index")
                .with("name", getOr(path, "Dude"))
                .with("now", LocalDateTime.now());
    }

    private String getOr(String name, String elseValue) {
        return Optional.ofNullable(name)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(elseValue);
    }
}