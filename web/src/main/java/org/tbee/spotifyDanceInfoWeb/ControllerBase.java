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

public class ControllerBase {

    private static final Logger logger = LoggerFactory.getLogger(ControllerBase.class);

    protected SpotifyConnectData spotifyConnectData(HttpSession session) {
        String attributeName = "SpotifyConnectData";
        SpotifyConnectData spotifyConnectData = (SpotifyConnectData) session.getAttribute(attributeName);
        if (spotifyConnectData == null) {
            spotifyConnectData = new SpotifyConnectData();
            session.setAttribute(attributeName, spotifyConnectData);
        }
        return spotifyConnectData;
    }

    protected SpotifyApi spotifyApi(HttpSession session) {
        try {
            SpotifyConnectData spotifyConnectData = spotifyConnectData(session);

            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(spotifyConnectData.clientId())
                    .setClientSecret(spotifyConnectData.clientSecret())
                    .setRedirectUri(new URI(spotifyConnectData.redirectUrl()))
                    .setRefreshToken(spotifyConnectData.refreshToken())
                    .setAccessToken(spotifyConnectData.accessToken())
                    .build();

            if (spotifyConnectData.accessTokenExpireDateTime() != null && spotifyConnectData.accessTokenExpireDateTime().isBefore(LocalDateTime.now())) {
                refreshAccessToken(session, spotifyConnectData, spotifyApi);
            }

            return spotifyApi;
        }
        catch (URISyntaxException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    protected void refreshAccessToken(HttpSession session) {
        refreshAccessToken(session, spotifyConnectData(session), spotifyApi(session));
    }

    protected void refreshAccessToken(HttpSession session, SpotifyConnectData spotifyConnectData, SpotifyApi spotifyApi) {
        try {
            if (logger.isInfoEnabled()) logger.info("Refreshing access token");
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build().execute();
            LocalDateTime expiresAt = expiresAt(authorizationCodeCredentials.getExpiresIn());
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken())
                    .accessTokenExpireDateTime(expiresAt);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logException(session, e);
        }
    }

    protected LocalDateTime expiresAt(int expiresIn) {
        return LocalDateTime.now().plusSeconds(expiresIn).minusMinutes(10);
    }

    protected <T> T logException(HttpSession session, Throwable t) {
        t.printStackTrace();

        // just in case something went wrong with scheduled refreshing
        if (t.getMessage().contains("The access token expired")) {
            refreshAccessToken(session);
        }

        return null;
    }
}
