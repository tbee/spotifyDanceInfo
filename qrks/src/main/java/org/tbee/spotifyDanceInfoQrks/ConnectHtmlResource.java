package org.tbee.spotifyDanceInfoQrks;

import groovy.util.logging.Log;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.ParseException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jboss.resteasy.plugins.providers.html.Renderable;
import org.tbee.spotifyDanceInfo.Cfg;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log
@Path("")
@Produces(MediaType.TEXT_HTML)
public class ConnectHtmlResource extends ResourceBase {

    @Inject
    Thymeleaf thymeleaf;

    @GET
    @Path("")
    public Renderable connect(@Context HttpServletRequest httpServletRequest) {
        HttpSession session = httpServletRequest.getSession();
        Cfg cfg = SpotifyDanceInfoQrks.cfg();

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
    public Response connect(@Context HttpServletRequest httpServletRequest, List<EntityPart> parts) throws URISyntaxException {
        try {
            HttpSession session = httpServletRequest.getSession();
            String clientId = getPartAsString("clientId", parts);
            String clientSecret = getPartAsString("clientSecret", parts);
            String redirectUrl = getPartAsString("redirectUrl", parts);
            EntityPart file = getPart("file", parts);

            // store connection data
            spotifyConnectData(session)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .redirectUrl(redirectUrl)
                    .connectTime(LocalDateTime.now());

            // Spotify API
            SpotifyApi spotifyApi = spotifyApi(session);

            // Load configuration
            Cfg cfg = new Cfg("session", false);
            session.setAttribute("cfg", cfg);
            String originalFilename = file.getFileName().orElse("");
            if (originalFilename.endsWith(".tsv")) {
                cfg.readMoreTracksTSV("web", file.getContent(), 0, 1);
            }
            else if (originalFilename.endsWith(".xlsx")) {
                cfg.readMoreTracksExcel("web", new XSSFWorkbook(file.getContent()), 0, 0, 1);
            }
            else if (originalFilename.endsWith(".xls")) {
                cfg.readMoreTracksExcel("web", new HSSFWorkbook(file.getContent()), 0, 0, 1);
            }

            // Forward to Spotify
            URI authorizationCodeUri = spotifyApi.authorizationCodeUri()
                    .scope("user-read-playback-state,user-read-currently-playing")
                    .build().execute();
            return Response.status(Response.Status.FOUND).location(authorizationCodeUri).build(); // redirect not working
        }
        catch (IOException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }

    }

    private EntityPart getPart(String id, List<EntityPart> parts) {
        return parts.stream().filter(p -> p.getName().equals(id)).findFirst().orElseThrow();
    }

    private String getPartAsString(String id, List<EntityPart> parts) {
        try {
            return getPart(id, parts).getContent(String.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getOr(String name, String elseValue) {
        return Optional.ofNullable(name)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(elseValue);
    }


    @GET
    @Path("/spotifyCallback")
    public Response spotifyCallback(@Context HttpServletRequest httpServletRequest, @QueryParam("code") @DefaultValue("") String authorizationCode) { //
        try {
            HttpSession session = httpServletRequest.getSession();
            SpotifyApi spotifyApi = spotifyApi(session);
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCode(authorizationCode).build().execute();
            LocalDateTime expiresAt = expiresAt(authorizationCodeCredentials.getExpiresIn());

            SpotifyConnectData spotifyConnectData = spotifyConnectData(session);
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken())
                    .accessTokenExpireDateTime(expiresAt);

            return Response.status(Response.Status.FOUND).location(new URI("/spotify")).build(); // redirect not working
        }
        catch (IOException | SpotifyWebApiException | ParseException | URISyntaxException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    public static class ConnectForm {

        private String clientId;
        private String clientSecret;
        private String redirectUrl;
        private List<Cfg.Abbreviation> abbreviations;

        public ConnectForm() {}

        public ConnectForm(String clientId, String clientSecret, String redirectUrl) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.redirectUrl = redirectUrl;
        }

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