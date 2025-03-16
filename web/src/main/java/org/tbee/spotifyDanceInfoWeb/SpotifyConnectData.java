package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class SpotifyConnectData {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyConnectData.class);

    static public SpotifyConnectData get(HttpSession session) {
        return (SpotifyConnectData) session.getAttribute(SpotifyConnectData.class.getName());
    }

    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private String refreshToken;
    private String accessToken;
    private LocalDateTime accessTokenExpireDateTime;
    private LocalDateTime connectTime = null;
    private AtomicInteger counter = new AtomicInteger(0);


    public SpotifyConnectData(HttpSession session) {
        session.setAttribute(SpotifyConnectData.class.getName(), this);
    }

    public AtomicInteger counter() {
        return counter;
    }

    public String clientId() {
        return clientId;
    }

    public SpotifyConnectData clientId(String v) {
        this.clientId = v;
        return this;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public SpotifyConnectData clientSecret(String v) {
        this.clientSecret = v;
        return this;
    }

    public String redirectUrl() {
        return redirectUrl;
    }

    public SpotifyConnectData redirectUrl(String v) {
        this.redirectUrl = v;
        return this;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public SpotifyConnectData refreshToken(String v) {
        this.refreshToken = v;
        return this;
    }

    public String accessToken() {
        return accessToken;
    }

    public SpotifyConnectData accessToken(String v) {
        this.accessToken = v;
        return this;
    }

    public LocalDateTime accessTokenExpireDateTime() {
        return accessTokenExpireDateTime;
    }

    public SpotifyConnectData accessTokenExpireDateTime(LocalDateTime v) {
        this.accessTokenExpireDateTime = v;
        return this;
    }


    public LocalDateTime connectTime() {
        return connectTime;
    }
    public SpotifyConnectData connectTime(LocalDateTime v) {
        this.connectTime = v;
        return this;
    }

    public SpotifyApi newApi() {
        try {
            return new SpotifyApi.Builder()
                        .setClientId(clientId())
                        .setClientSecret(clientSecret())
                        .setRedirectUri(new URI(redirectUrl()))
                        .setRefreshToken(refreshToken())
                        .setAccessToken(accessToken())
                        .build();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    public void refreshAccessToken(HttpSession session) {
        try {
            if (logger.isInfoEnabled()) logger.info("Refreshing access token");
            AuthorizationCodeCredentials authorizationCodeCredentials = get(session).newApi().authorizationCodeRefresh().build().execute();
            LocalDateTime expiresAt = calculateExpiresAt(authorizationCodeCredentials.getExpiresIn());
            refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : refreshToken());
            accessToken(authorizationCodeCredentials.getAccessToken());
            if (logger.isDebugEnabled()) logger.debug("accessToken: refreshed " + accessToken);
            accessTokenExpireDateTime(expiresAt);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            ControllerBase.logException(e);
        }
    }

    protected LocalDateTime calculateExpiresAt(int expiresIn) {
        return LocalDateTime.now().plusSeconds(expiresIn).minusMinutes(10);
    }

}
