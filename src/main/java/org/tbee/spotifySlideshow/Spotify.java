package org.tbee.spotifySlideshow;

import org.apache.hc.core5.http.ParseException;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.CurrentlyPlayingType;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

public class Spotify {

    private final boolean simulationMode;

    private SpotifyApi spotifyApi = null;

    public Spotify(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    public void connect() {
        if (simulationMode) {
            return;
        }

        try {
            TECL tecl = SpotifySlideshow.tecl();

            // Setup the API
            String clientId = tecl.str("/spotify/clientId", "");
            String clientSecret = tecl.str("/spotify/clientSecret", "");
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectUri(new URI("https://www.tbee.org"))
                    .build();

            // Do we have tokens stored or need to fetch them?
            String accessToken = tecl.str("/spotify/accessToken", "");
            String refreshToken = tecl.str("/spotify/refreshToken", "");
            if (accessToken.isBlank()) {

                // https://developer.spotify.com/documentation/web-api/concepts/authorization
                // The authorizationCodeUri must be opened in the browser, the resulting code (in the redirect URL) pasted into the popup
                // The code can only be used once to connect
                URI authorizationCodeUri = spotifyApi.authorizationCodeUri()
                        .scope("user-read-playback-state,user-read-currently-playing")
                        .build().execute();
                System.out.println("authorizationCodeUri " + authorizationCodeUri);
                Desktop.getDesktop().browse(authorizationCodeUri);

                var authorizationCode = javax.swing.JOptionPane.showInputDialog("Please copy the authorization code here");
                if (authorizationCode == null || authorizationCode.isBlank()) {
                    String message = "Authorization code cannot be empty";
                    javax.swing.JOptionPane.showMessageDialog(null, message);
                    throw new IllegalArgumentException(message);
                }

                AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCode(authorizationCode).build().execute();
                accessToken = authorizationCodeCredentials.getAccessToken();
                System.out.println("accessToken " + accessToken);
                refreshToken = authorizationCodeCredentials.getRefreshToken();
                System.out.println("refreshToken " + refreshToken);
            }
            spotifyApi.setAccessToken(accessToken);
            spotifyApi.setRefreshToken(refreshToken);
        }
        catch (IOException | URISyntaxException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem starting SportifySlideshow", e);
        }
    }

    public CurrentlyPlaying getUsersCurrentlyPlayingTrack() {
        if (simulationMode) {
            List<CurrentlyPlaying> tracks = List.of(new CurrentlyPlaying.Builder()
                            .setCurrentlyPlayingType(CurrentlyPlayingType.TRACK)
                            .setIs_playing(true)
                            .setItem(new Track.Builder()
                                    .setId("1cqQYoFfwCisUAhEy1JoRr")
                                    .setName("I Will Wait For You")
                                    .build()
                            ).build()
                    , new CurrentlyPlaying.Builder()
                            .setCurrentlyPlayingType(CurrentlyPlayingType.TRACK)
                            .setIs_playing(true)
                            .setItem(new Track.Builder()
                                    .setId("rtgh453t45hftgh45gfg54")
                                    .setName("Testing 1-2")
                                    .build()
                            ).build()
                    , new CurrentlyPlaying.Builder()
                            .setCurrentlyPlayingType(CurrentlyPlayingType.TRACK)
                            .setIs_playing(true)
                            .setItem(new Track.Builder()
                                    .setId("454")
                                    .setName("Undefined")
                                    .build()
                            ).build()
            );
            return tracks.get(new Random().nextInt(tracks.size()));
        }

        try {
            CurrentlyPlaying currentlyPlaying = spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
            return currentlyPlaying;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem starting SportifySlideshow", e);
        }
    }
}
