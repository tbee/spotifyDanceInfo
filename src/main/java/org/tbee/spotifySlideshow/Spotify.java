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

            System.out.println("clientId " + tecl.str("/spotify/clientId"));
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(tecl.str("/spotify/clientId"))
                    .setClientSecret(tecl.str("/spotify/clientSecret"))
                    .setRedirectUri(new URI("https://www.tbee.org"))
                    .build();

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
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
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
