package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

public class SpotifyConnectData implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotifyConnectData.class);

    static public SpotifyConnectData get(HttpSession session) {
        SpotifyConnectData spotifyConnectData = (SpotifyConnectData) session.getAttribute(SpotifyConnectData.class.getName());
        if (LOGGER.isDebugEnabled()) LOGGER.debug("SpotifyConnectData retrieved from HTTP session " + session.getId() + " -> " + spotifyConnectData);
        return spotifyConnectData;
    }

    public SpotifyConnectData storeIn(HttpSession session) {
        session.setAttribute(SpotifyConnectData.class.getName(), this);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("SpotifyConnectData stored in HTTP session " + session.getId() + " -> " + this);
        return this;
    }

    static public SpotifyConnectData deserialize(String base64) {
        try { // TODO: encrypt
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            SpotifyConnectData spotifyConnectData = (SpotifyConnectData) objectInputStream.readObject();
            objectInputStream.close();
            return spotifyConnectData;
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String serialize() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.flush();
            objectOutputStream.close();
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private String refreshToken;
    private String accessToken;
    private LocalDateTime accessTokenExpireDateTime;
    private LocalDateTime connectTime = null;
    private AtomicInteger counter = new AtomicInteger(0);

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
        if (LOGGER.isInfoEnabled()) LOGGER.debug("accesstoken: got from spotify " + v);
        if (LOGGER.isInfoEnabled()) LOGGER.debug("accesstoken: stored in " + this);
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
            if (LOGGER.isInfoEnabled()) LOGGER.info("Refreshing access token");
            AuthorizationCodeCredentials authorizationCodeCredentials = get(session).newApi().authorizationCodeRefresh().build().execute();
            LocalDateTime expiresAt = calculateExpiresAt(authorizationCodeCredentials.getExpiresIn());
            refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : refreshToken());
            accessToken(authorizationCodeCredentials.getAccessToken());
            if (LOGGER.isDebugEnabled()) LOGGER.debug("accessToken: refreshed " + accessToken);
            accessTokenExpireDateTime(expiresAt);

            storeIn(session); // on redis this is required
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            ControllerBase.logException(e);
        }
    }

    protected LocalDateTime calculateExpiresAt(int expiresIn) {
        return LocalDateTime.now().plusSeconds(expiresIn).minusMinutes(10);
    }

}
