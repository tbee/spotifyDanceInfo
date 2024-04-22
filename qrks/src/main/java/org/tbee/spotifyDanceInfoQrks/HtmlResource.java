package org.tbee.spotifyDanceInfoQrks;

import groovy.util.logging.Log;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import org.jboss.resteasy.plugins.providers.html.Renderable;
import org.tbee.spotifyDanceInfo.Cfg;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
    public Renderable connect(@Context HttpServletRequest httpServletRequest, @QueryParam("name") @DefaultValue("Buddy") String name) {

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


    @POST
    @Path("")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response connect(@MultipartForm ConnectForm connectForm) throws URISyntaxException {
//        LOGGER.log(Level.INFO, "name: {0} ", name);
//        LOGGER.log(
//                Level.INFO,
//                "uploading file: {0},{1},{2},{3}",
//                new Object[]{
//                        part.getMediaType(),
//                        part.getName(),
//                        part.getFileName(),
//                        part.getHeaders()
//                }
//        );
//        try {
//            Files.copy(
//                    part.getContent(),
//                    Paths.get(uploadedPath.toString(), part.getFileName().orElse(generateFileName(UUID.randomUUID().toString(), mediaTypeToFileExtension(part.getMediaType())))),
//                    StandardCopyOption.REPLACE_EXISTING
//            );
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return Response.ok().build();
        return Response.temporaryRedirect(new URI("/")).build(); // redirect not working
    }

    private String getOr(String name, String elseValue) {
        return Optional.ofNullable(name)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(elseValue);
    }

    public static class ConnectForm {

        @FormParam("clientId")
        private String clientId;
        @FormParam("clientSecret")
        private String clientSecret;
        @FormParam("redirectUrl")
        private String redirectUrl;
        private List<Cfg.Abbreviation> abbreviations;

        @FormParam("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream file;

        @FormParam("fileName")
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName;

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