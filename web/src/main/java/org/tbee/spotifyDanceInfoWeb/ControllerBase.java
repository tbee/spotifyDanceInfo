package org.tbee.spotifyDanceInfoWeb;

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

    protected SpotifyApi spotifyApi() {
        try {
            SpotifyConnectData spotifyConnectData = SpotifyConnectData.get();

            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(spotifyConnectData.clientId())
                    .setClientSecret(spotifyConnectData.clientSecret())
                    .setRedirectUri(new URI(spotifyConnectData.redirectUrl()))
                    .setRefreshToken(spotifyConnectData.refreshToken())
                    .setAccessToken(spotifyConnectData.accessToken())
                    .build();

            if (spotifyConnectData.accessTokenExpireDateTime() != null && spotifyConnectData.accessTokenExpireDateTime().isBefore(LocalDateTime.now())) {
                refreshAccessToken(spotifyConnectData, spotifyApi);
            }

            return spotifyApi;
        }
        catch (URISyntaxException e) {
            throw new RuntimeException("Problem connecting to Spotify webapi", e);
        }
    }

    protected void refreshAccessToken() {
        refreshAccessToken(SpotifyConnectData.get(), spotifyApi());
    }

    protected void refreshAccessToken(SpotifyConnectData spotifyConnectData, SpotifyApi spotifyApi) {
        try {
            if (logger.isInfoEnabled()) logger.info("Refreshing access token");
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build().execute();
            LocalDateTime expiresAt = expiresAt(authorizationCodeCredentials.getExpiresIn());
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken())
                    .accessTokenExpireDateTime(expiresAt);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logException(e);
        }
    }

    protected LocalDateTime expiresAt(int expiresIn) {
        return LocalDateTime.now().plusSeconds(expiresIn).minusMinutes(10);
    }

    protected <T> T logException(Throwable t) {
        t.printStackTrace();

        // just in case something went wrong with scheduled refreshing
        if (t.getMessage().contains("The access token expired")) {
            refreshAccessToken();
        }

        return null;
    }
}
