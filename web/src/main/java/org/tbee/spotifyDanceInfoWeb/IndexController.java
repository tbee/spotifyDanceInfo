package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index(Model model) {
        ConnectForm connectForm = new ConnectForm();
        model.addAttribute("ConnectForm", connectForm);

        Cfg cfg = new Cfg();
        if (cfg.webapiRefreshToken() != null) {
            connectForm.setClientId(cfg.webapiClientId());
            connectForm.setClientSecret(cfg.webapiClientSecret());
            connectForm.setRedirectUrl("http://localhost:8080/spotifyCallback"); // TBEERNOT generate URL
            connectForm.setRefreshToken(cfg.webapiRefreshToken());
        }
        return "index";
    }

    @PostMapping("/")
    public String indexSubmit(HttpSession session, Model model, @ModelAttribute ConnectForm connectForm) {
        try {
            SpotifyConnectData spotifyConnectData = new SpotifyConnectData()
                    .clientId(connectForm.getClientId())
                    .clientSecret(connectForm.getClientSecret())
                    .redirectUrl(connectForm.getRedirectUrl())
                    .refreshToken(connectForm.getRefreshToken().isBlank() ? null : connectForm.getRefreshToken());
            session.setAttribute("SpotifyConnectData", spotifyConnectData);

            // Spotify API
            SpotifyApi spotifyApi = spotifyApi(session);

            // Forward to Spotify
            if (connectForm.getRefreshToken().isBlank()) {
                URI authorizationCodeUri = spotifyApi.authorizationCodeUri()
                        .scope("user-read-playback-state,user-read-currently-playing")
                        .build().execute();
                return "redirect:" + authorizationCodeUri.toURL().toString();
            }

            // Get the access token
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build().execute();

            // Update connect data
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken());

            return "redirect:/spotify";
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
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

    @GetMapping("/spotify")
    public String spotify(Model model) {
        return "spotify";
    }

    private SpotifyApi spotifyApi(HttpSession session) {
        try {
            SpotifyConnectData spotifyConnectData = spotifyConnectData(session);
            return new SpotifyApi.Builder()
                    .setClientId(spotifyConnectData.clientId())
                    .setClientSecret(spotifyConnectData.clientSecret())
                    .setRedirectUri(new URI(spotifyConnectData.redirectUrl()))
                    .setRefreshToken(spotifyConnectData.refreshToken())
                    .build();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    private static SpotifyConnectData spotifyConnectData(HttpSession session) {
        SpotifyConnectData spotifyConnectData = (SpotifyConnectData) session.getAttribute("SpotifyConnectData");
        return spotifyConnectData;
    }

    public static class SpotifyConnectData {
        private String clientId;
        private String clientSecret;
        private String redirectUrl;
        private String refreshToken;
        private String accessToken;

        public String clientId() {
            return clientId;
        }

        public SpotifyConnectData clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public String clientSecret() {
            return clientSecret;
        }

        public SpotifyConnectData clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public String redirectUrl() {
            return redirectUrl;
        }

        public SpotifyConnectData redirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
            return this;
        }

        public String refreshToken() {
            return refreshToken;
        }

        public SpotifyConnectData refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public String accessToken() {
            return accessToken;
        }

        public SpotifyConnectData accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
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

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
