package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ParseException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ConnectController extends ControllerBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectController.class);

    // Environment variables have higher priority than most application files, but lower than command-line arguments or system properties (in recent versions).
    //    Replace dots (.) with underscores (_).
    //    Remove dashes (-).
    //    Convert to uppercase.
    // So:
    // - baseUrl -> BASEURL
    @Autowired
    private Environment environment;

    @GetMapping("/")
    public String connect(HttpSession session, HttpServletRequest request, Model model) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("/connect session=" + session.getId());

        String baseUrl = baseUrl();
        String requestURL = request.getRequestURL().toString();
        if (!requestURL.equals(baseUrl) && !requestURL.equals(baseUrl + "/")) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("We're not at the baseurl, redirecting (" + requestURL + ")");
            return "redirect:" + baseUrl;
        }

        try {
            setVersion(model);
            ConnectForm connectForm = new ConnectForm();
            connectForm.setRedirectUrl(baseUrl + "/spotifyCallback");
            model.addAttribute("ConnectForm", connectForm);

            // If set, prepopulate the form (for development mainly)
            CfgApp cfg = SpotifyDanceInfoWebApplication.cfg();
            if (cfg.webapiClientId() != null) {
                connectForm.setClientId(cfg.webapiClientId());
            }
            if (cfg.webapiClientSecret() != null) {
                connectForm.setClientSecret(cfg.webapiClientSecret());
            }

            connectForm.abbreviations(cfg.getListofDanceAbbreviations());

            return "connect";
        }
        catch (RuntimeException e) {
            LOGGER.error("Ohoh", e);
            throw e;
        }
    }

    @PostMapping("/")
    public String connectSubmit(HttpSession session, Model model, @ModelAttribute ConnectForm connectForm, @RequestParam("file") MultipartFile file) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("/connectSubmit session=" + session.getId());
        try {
            // Load submitted configuration
            // Each time CfgSession is created, it will load the config.tecl.
            // So every CfgSession contains the application level information, and is then augmented with the uploaded data.
            // This also facilitates that if one of the external sources is altered, a login suffices to get the latest.
            CfgSession cfg = new CfgSession().storeIn(session).readMoreTracks();
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                // do nothing
            }
            else if (originalFilename.endsWith(".tsv")) {
                cfg.readMoreTracksTSV("web", file.getInputStream(), 0, 1);
            }
            else if (originalFilename.endsWith(".xlsx")) {
                cfg.readMoreTracksExcel("web", new XSSFWorkbook(file.getInputStream()), 0, 0, 1);
            }
            else if (originalFilename.endsWith(".xls")) {
                cfg.readMoreTracksExcel("web", new HSSFWorkbook(file.getInputStream()), 0, 0, 1);
            }

            // store connection data
            SpotifyConnectData spotifyConnectData = new SpotifyConnectData()
                    .clientId(connectForm.getClientId())
                    .clientSecret(connectForm.getClientSecret())
                    .redirectUrl(connectForm.getRedirectUrl())
                    .connectTime(LocalDateTime.now())
                    .storeIn(session);

            // Forward to Spotify
            // https://developer.spotify.com/documentation/web-api/concepts/scopes
            URI authorizationCodeUri = spotifyConnectData.newApi().authorizationCodeUri()
                    .scope("user-read-playback-state,user-read-currently-playing")
                    //.state(spotifyConnectData.serialize())
                    .build().execute();
            return "redirect:" + authorizationCodeUri.toURL();
        }
        catch (IOException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
        catch (RuntimeException e) {
            LOGGER.error("Ohoh", e);
            throw e;
        }
    }

    @GetMapping("/spotifyCallback")
    public String spotifyCallback(HttpSession session, @RequestParam("code") String authorizationCode
            , @RequestParam(name = "spotifyOauthState", defaultValue = "") String spotifyOauthState
    ) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("/spotifyCallback session=" + session.getId());
        try {
            CfgSession cfgSession = CfgSession.get(session);
            SpotifyConnectData spotifyConnectData = SpotifyConnectData.get(session);

            // Spotify has accepted the connection, remember the details
            SpotifyApi spotifyApi = spotifyConnectData.newApi();
            cfgSession.rateLimiterRemaining().claim("authorizationCode");
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCode(authorizationCode).build().execute();
            LocalDateTime expiresAt = spotifyConnectData.calculateExpiresAt(authorizationCodeCredentials.getExpiresIn());
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken())
                    .accessTokenExpireDateTime(expiresAt)
                    .storeIn(session);  // on redis this is required

            // Create and store the object that holds the screen data
            ScreenData screenData = new ScreenData().storeIn(session);

            // Now that the spotify API is active, read config data that requires spotify access
            cfgSession
                    .onChange(cfg -> {
                        screenData.refresh(cfg).storeIn(session); // TBEERNOT, the storeIn of course is not working, the session object is already expired.
                    })
                    .readPlaylists(spotifyConnectData::newApi);

            // redirect to our spotify page, start showing the track information
            String redirectUrl = String.format("redirect:%s/spotify", baseUrl());
            if (LOGGER.isInfoEnabled()) LOGGER.info("Redirect to spofity play page, redirectUrl=" + redirectUrl);
            return redirectUrl;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
        catch (RuntimeException e) {
            LOGGER.error("Oh oh", e);
            throw e;
        }
    }

    @Nullable
    private String baseUrl() {
        String baseUrl = environment.getProperty("baseUrl");
        if (LOGGER.isDebugEnabled()) LOGGER.debug("BaseUrl=" + baseUrl);
        return baseUrl;
    }

    @GetMapping("/example.{filetype}")
    public void example(HttpServletResponse response, @PathVariable("filetype") String filetype) throws IOException {
        try {
            InputStream inputStream = CfgSession.class.getResourceAsStream("/trackToDance." + filetype); // fetch resource from shared jar
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
            response.setContentType(switch (filetype) {
                case "tsv" -> "text/tab-separated-values";
                case "xls" -> "application/vnd.ms-excel";
                case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                default -> throw new IllegalArgumentException("Unknown filetype: " + filetype);
            });
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"example." + filetype + "\"");
        }
        catch (RuntimeException e) {
            LOGGER.error("Oh oh", e);
            throw e;
        }
    }

    public static class ConnectForm {
        private String clientId;
        private String clientSecret;
        private String redirectUrl;
        private List<CfgSession.Abbreviation> abbreviations;

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

        public List<CfgSession.Abbreviation> abbreviations() {
            return abbreviations;
        }
        public ConnectForm abbreviations(List<CfgSession.Abbreviation> abbreviations) {
            this.abbreviations = abbreviations;
            return this;
        }
    }
}
