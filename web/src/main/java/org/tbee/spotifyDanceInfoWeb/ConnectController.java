package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ParseException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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

    private static final Logger logger = LoggerFactory.getLogger(ConnectController.class);

    @GetMapping("/")
    public String connect(HttpServletRequest request, Model model) {
        ConnectForm connectForm = new ConnectForm();
        model.addAttribute("ConnectForm", connectForm);

        // If set, prepopulate the form (for development mainly)
        CfgApp cfg = SpotifyDanceInfoWebApplication.cfg();
        if (cfg.webapiRefreshToken() != null) {
            connectForm.setClientId(cfg.webapiClientId());
            connectForm.setClientSecret(cfg.webapiClientSecret());
            String webapiRedirect = cfg.webapiRedirect();
            connectForm.setRedirectUrl(webapiRedirect != null && !webapiRedirect.isBlank() ? webapiRedirect : request.getRequestURL().toString() + "spotifyCallback");
        }

        connectForm.abbreviations(cfg.getListofDanceAbbreviations());

        return "connect";
    }

    @PostMapping("/")
    public String connectSubmit(Model model, @ModelAttribute ConnectForm connectForm, @RequestParam("file") MultipartFile file) {
        try {
            // Load submitted configuration
            CfgSession cfg = new CfgSession("session", false, false);
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
            SpotifyConnectData.get()
                    .clientId(connectForm.getClientId())
                    .clientSecret(connectForm.getClientSecret())
                    .redirectUrl(connectForm.getRedirectUrl())
                    .connectTime(LocalDateTime.now());

            // Spotify API
            SpotifyApi spotifyApi = spotifyApi();

            // Forward to Spotify
            // https://developer.spotify.com/documentation/web-api/concepts/scopes
            URI authorizationCodeUri = spotifyApi.authorizationCodeUri()
                    .scope("user-read-playback-state,user-read-currently-playing")
                    .build().execute();
            return "redirect:" + authorizationCodeUri.toURL();
        }
        catch (IOException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    @GetMapping("/spotifyCallback")
    public String spotifyCallback(@RequestParam("code") String authorizationCode) {
        try {
            SpotifyApi spotifyApi = spotifyApi();
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCode(authorizationCode).build().execute();
            LocalDateTime expiresAt = expiresAt(authorizationCodeCredentials.getExpiresIn());

            SpotifyConnectData spotifyConnectData = SpotifyConnectData.get();
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken())
                    .accessTokenExpireDateTime(expiresAt);

            // Now that the spotify API is active, read config data that requires spotify access
//            SpotifyDanceInfoWebApplication.cfg().readPlaylists(spotifyApi); // this is done once for the whole application
//            CfgSession.get().readPlaylists(spotifyApi);

            return "redirect:/spotify";
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }


    @GetMapping("/example.{filetype}")
    public void example(HttpServletResponse response, @PathVariable("filetype") String filetype) throws IOException {
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
