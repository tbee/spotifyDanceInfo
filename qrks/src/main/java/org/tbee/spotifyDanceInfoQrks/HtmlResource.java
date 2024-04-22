package org.tbee.spotifyDanceInfoQrks;

import groovy.util.logging.Log;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.plugins.providers.html.Renderable;
import org.tbee.spotifyDanceInfo.Cfg;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log
@Path("")
@Produces(MediaType.TEXT_HTML)
public class HtmlResource {

    static private Cfg cfg = new Cfg();

    @Inject
    Thymeleaf thymeleaf;

    @GET
    @Path("")
    public Renderable query(@Context HttpServletRequest httpServletRequest, @QueryParam("name") @DefaultValue("Buddy") String name) {

        ConnectForm connectForm = new ConnectForm();
        connectForm.abbreviations(cfg.getListofDanceAbbreviations());

        if (cfg.webapiRefreshToken() != null) {
            connectForm.setClientId(cfg.webapiClientId());
            connectForm.setClientSecret(cfg.webapiClientSecret());
            String webapiRedirect = cfg.webapiRedirect();
            connectForm.setRedirectUrl(webapiRedirect != null && !webapiRedirect.isBlank() ? webapiRedirect : httpServletRequest.getRequestURL().toString() + "spotifyCallback");
        }

        return thymeleaf.template("connect")
                .with("ConnectForm", connectForm);
    }

    @GET
    @Path("{path}") // @Path("{path: .*}")
    public Renderable path(@PathParam("path") String path) {
        return thymeleaf.template("index")
                .with("name", getOr(path, "Dude"))
                .with("now", LocalDateTime.now());
    }

    private String getOr(String name, String elseValue) {
        return Optional.ofNullable(name)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(elseValue);
    }

    public static class ConnectForm {
        private String clientId;
        private String clientSecret;
        private String redirectUrl;
        private List<Cfg.Abbreviation> abbreviations;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        public List<Cfg.Abbreviation> abbreviations() {
            return abbreviations;
        }
        public ConnectForm abbreviations(List<Cfg.Abbreviation> abbreviations) {
            this.abbreviations = abbreviations;
            return this;
        }
    }
}