package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletRequest;
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
    public String index(HttpServletRequest request, Model model) {
        ConnectForm connectForm = new ConnectForm();
        model.addAttribute("ConnectForm", connectForm);

        Cfg cfg = new Cfg();
        if (cfg.webapiRefreshToken() != null) {
            connectForm.setClientId(cfg.webapiClientId());
            connectForm.setClientSecret(cfg.webapiClientSecret());
            connectForm.setRedirectUrl(request.getRequestURL().toString() + "spotifyCallback"); // TBEERNOT generate URL
        }

        return "index";
    }

    @PostMapping("/")
    public String indexSubmit(HttpSession session, Model model, @ModelAttribute ConnectForm connectForm) {
        try {
            SpotifyConnectData spotifyConnectData = new SpotifyConnectData()
                    .clientId(connectForm.getClientId())
                    .clientSecret(connectForm.getClientSecret())
                    .redirectUrl(connectForm.getRedirectUrl());
            session.setAttribute("SpotifyConnectData", spotifyConnectData);

            // Spotify API
            SpotifyApi spotifyApi = spotifyApi(session);

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
                    .setAccessToken(spotifyConnectData.accessToken())
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
