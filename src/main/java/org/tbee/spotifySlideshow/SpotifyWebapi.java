package org.tbee.spotifySlideshow;

import org.apache.hc.core5.http.ParseException;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.CurrentlyPlayingType;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.special.PlaybackQueue;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.awt.Desktop;
import java.awt.Window;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class SpotifyWebapi {

    private final boolean simulationMode;

    private SpotifyApi spotifyApi = null;

    public SpotifyWebapi(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    public void connect() {
        if (simulationMode) {
            return;
        }

        try {
            TECL tecl = SpotifySlideshow.tecl();
            TECL webapiTecl = tecl.grp("/webapi");

            // Setup the API
            String clientId = webapiTecl.str("clientId", "");
            String clientSecret = webapiTecl.str("clientSecret", "");
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectUri(new URI(webapiTecl.str("redirect", "")))
                    .build();

            // Do we have tokens stored or need to fetch them?
            String accessToken;
            String refreshToken = webapiTecl.str("refreshToken", "");
            if (!refreshToken.isBlank()) {
                spotifyApi.setRefreshToken(refreshToken);
                AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build().execute();
                accessToken = authorizationCodeCredentials.getAccessToken();
            }
            else {

                // https://developer.spotify.com/documentation/web-api/concepts/authorization
                // The authorizationCodeUri must be opened in the browser, the resulting code (in the redirect URL) pasted into the popup
                // The code can only be used once to connect
                URI authorizationCodeUri = spotifyApi.authorizationCodeUri()
                        .scope("user-read-playback-state,user-read-currently-playing")
                        .build().execute();
                System.out.println("authorizationCodeUri " + authorizationCodeUri);
                Desktop.getDesktop().browse(authorizationCodeUri);

                var authorizationCode = javax.swing.JOptionPane.showInputDialog(Window.getWindows()[0], "Please copy the authorization code here");
                if (authorizationCode == null || authorizationCode.isBlank()) {
                    String message = "Authorization code cannot be empty";
                    javax.swing.JOptionPane.showMessageDialog(Window.getWindows()[0], message);
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
            throw new RuntimeException("Problem connecting to Sportify webapi", e);
        }
    }

    synchronized public void getPlaybackQueue(Consumer<List<Song>> callback) throws IOException, ParseException, SpotifyWebApiException {
        List<Song> songs = new ArrayList<>();
        //System.out.println(LocalDateTime.now());
        PlaybackQueue playbackQueue = spotifyApi.getTheUsersQueue().build().execute();
        List<IPlaylistItem> playbackQueueContents = playbackQueue.getQueue();
        for (IPlaylistItem playlistItem : playbackQueue.getQueue()) {
            //System.out.println("    | " + playlistItem.getId() + " | \"" + playlistItem.getName() + "\" | # " + playlistItem.getHref());
            songs.add(new Song(playlistItem.getId(), "", playlistItem.getName()));
        }
        //System.out.println(LocalDateTime.now());
        callback.accept(songs);
    }


    synchronized public void getCoverArt(String trackId, Consumer<URL> callback) throws IOException, ParseException, SpotifyWebApiException {
        Track track = spotifyApi.getTrack(trackId).build().execute();
        Image[] images = track.getAlbum().getImages();
        //Arrays.stream(images).forEach(i -> System.out.println(i.getUrl() + " " + i.getWidth() + "x" + i.getHeight()));
        callback.accept(images.length == 0 ? null : new URL(images[0].getUrl()));
    }

    synchronized public CurrentlyPlaying getUsersCurrentlyPlayingTrack() {
        if (simulationMode) {
            List<CurrentlyPlaying> tracks = List.of(
                    currentlyPlaying("1cqQYoFfwCisUAhEy1JoRr", "I Will Wait For You"),
                    currentlyPlaying("rtgh453t45hftgh45gfg54", "Testing 1-2"),
                    currentlyPlaying("1111111111111111111111", "No name"),
                    currentlyPlaying("1cqQYoFfwCireerty1JoRr", "Hello"));
            return tracks.get(new Random().nextInt(tracks.size()));
        }

        try {
            try {
                return spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
            }
            catch (UnauthorizedException e) {
                // Try getting new tokens
                AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build().execute();
                String refreshToken = authorizationCodeCredentials.getRefreshToken();
                System.out.println("refreshToken " + refreshToken);
                spotifyApi.setRefreshToken(refreshToken);
                String accessToken = authorizationCodeCredentials.getAccessToken();
                System.out.println("accessToken " + accessToken);
                spotifyApi.setAccessToken(accessToken);

                // retry
                return spotifyApi.getUsersCurrentlyPlayingTrack().build().execute();
            }
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem fetching CurrentlyPlaying data", e);
        }
    }

    private CurrentlyPlaying currentlyPlaying(String id, String name) {
        return new CurrentlyPlaying.Builder()
                .setCurrentlyPlayingType(CurrentlyPlayingType.TRACK)
                .setIs_playing(true)
                .setItem(new Track.Builder()
                        .setId(id)
                        .setName(name)
                        .build()
                ).build();
    }
}
