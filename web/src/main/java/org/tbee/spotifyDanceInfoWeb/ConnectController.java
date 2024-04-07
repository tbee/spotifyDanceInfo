package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.hc.core5.http.ParseException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.tbee.spotifyDanceInfo.Cfg;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Controller
public class ConnectController {

    @GetMapping("/")
    public String connect(HttpServletRequest request, Model model) {
        ConnectForm connectForm = new ConnectForm();
        model.addAttribute("ConnectForm", connectForm);

        Cfg cfg = SpotifyDanceInfoWebApplication.cfg();
        if (cfg.webapiRefreshToken() != null) {
            connectForm.setClientId(cfg.webapiClientId());
            connectForm.setClientSecret(cfg.webapiClientSecret());
            String webapiRedirect = cfg.webapiRedirect();
            connectForm.setRedirectUrl(webapiRedirect != null ? webapiRedirect : request.getRequestURL().toString() + "spotifyCallback"); // TBEERNOT generate URL
        }

        return "connect";
    }

    @PostMapping("/")
    public String connectSubmit(HttpSession session, Model model, @ModelAttribute ConnectForm connectForm, @RequestParam("file") MultipartFile file) {
        try {
            // store connection data
            spotifyConnectData(session)
                    .clientId(connectForm.getClientId())
                    .clientSecret(connectForm.getClientSecret())
                    .redirectUrl(connectForm.getRedirectUrl());

            // Spotify API
            SpotifyApi spotifyApi = spotifyApi(session);

            // Load configuration
            Cfg cfg = new Cfg("session", false);
            session.setAttribute("cfg", cfg);
            String originalFilename = file.getOriginalFilename();
            if (originalFilename.endsWith(".tsv")) {
                cfg.readMoreTracksTSV("web", file.getInputStream(), 0, 1);
            }
            else if (originalFilename.endsWith(".xlsx")) {
                cfg.readMoreTracksExcel("web", new XSSFWorkbook(file.getInputStream()), 0, 0, 1);
            }
            else if (originalFilename.endsWith(".xls")) {
                cfg.readMoreTracksExcel("web", new HSSFWorkbook(file.getInputStream()), 0, 0, 1);
            }

            // Forward to Spotify
            URI authorizationCodeUri = spotifyApi.authorizationCodeUri()
                    .scope("user-read-playback-state,user-read-currently-playing")
                    .build().execute();
            return "redirect:" + authorizationCodeUri.toURL().toString();
        }
        catch (IOException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    @GetMapping("/spotifyCallback")
    public String spotifyCallback(HttpSession session, @RequestParam("code") String authorizationCode) {
        try {
            SpotifyApi spotifyApi = spotifyApi(session);
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCode(authorizationCode).build().execute();

            SpotifyConnectData spotifyConnectData = spotifyConnectData(session);
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken());

            return "redirect:/spotify";
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    static SpotifyConnectData spotifyConnectData(HttpSession session) {
        String attributeName = "SpotifyConnectData";
        SpotifyConnectData spotifyConnectData = (SpotifyConnectData) session.getAttribute(attributeName);
        if (spotifyConnectData == null) {
            spotifyConnectData = new SpotifyConnectData();
            session.setAttribute(attributeName, spotifyConnectData);
        }
        return spotifyConnectData;
    }

    static SpotifyApi spotifyApi(HttpSession session) {
        try {
            SpotifyConnectData spotifyConnectData = spotifyConnectData(session);
            return new SpotifyApi.Builder()
                    .setClientId(spotifyConnectData.clientId())
                    .setClientSecret(spotifyConnectData.clientSecret())
                    .setRedirectUri(new URI(spotifyConnectData.redirectUrl()))
                    .setRefreshToken(spotifyConnectData.refreshToken())
                    .setAccessToken(spotifyConnectData.accessToken())
                    .build();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    public static class ConnectForm {
        private String clientId;
        private String clientSecret;
        private String redirectUrl;
        private String refreshToken;

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
    }
}
