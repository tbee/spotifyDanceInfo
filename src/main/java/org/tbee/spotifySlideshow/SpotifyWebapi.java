package org.tbee.spotifySlideshow;

import org.apache.hc.core5.http.ParseException;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Image;

import java.awt.Desktop;
import java.awt.Window;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SpotifyWebapi implements Spotify {

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    private Consumer<Song> currentlyPlayingCallback = song -> {};
    private Consumer<List<Song>> nextUpCallback = songs -> {};
    private Consumer<URL> coverArtCallback = url -> {};

    private SpotifyApi spotifyApi = null;
    private Song currentPlayingSong = null;


    public SpotifyWebapi currentlyPlayingCallback(Consumer<Song> currentlyPlayingCallback) {
        this.currentlyPlayingCallback = currentlyPlayingCallback;
        return this;
    }
    public SpotifyWebapi nextUpCallback(Consumer<List<Song>> nextUpCallback) {
        this.nextUpCallback = nextUpCallback;
        return this;
    }
    public SpotifyWebapi coverArtCallback(Consumer<URL> coverArtCallback) {
        this.coverArtCallback = coverArtCallback;
        return this;
    }

    public void connect() {
        try {
            TECL tecl = SpotifySlideshow.tecl();
            TECL webapiTecl = tecl.grp("/spotify/webapi");

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

            // Start polling
            scheduledExecutorService.scheduleAtFixedRate(this::pollCurrentlyPlaying, 0, 3, TimeUnit.SECONDS);
        }
        catch (IOException | URISyntaxException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem connecting to Sportify webapi", e);
        }
    }


    public void pollCurrentlyPlaying() {
        spotifyApi.getUsersCurrentlyPlayingTrack().build().executeAsync()
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                })
                .thenAccept(currentlyPlaying -> {
                    boolean playing = (currentlyPlaying != null && currentlyPlaying.getIs_playing());
                    Song song = (!playing ? null : new Song(currentlyPlaying.getItem().getId(), "", currentlyPlaying.getItem().getName()));

                    boolean songChanged = !Objects.equals(this.currentPlayingSong, song);
                    if (!songChanged) {
                        return;
                    }
                    currentPlayingSong = song;

                    System.out.println("callback");
                    currentlyPlayingCallback.accept(song);
                    if (song == null) {
                        coverArtCallback.accept(null);
                        nextUpCallback.accept(List.of());
                    }
                    else {
                        scheduledExecutorService.schedule(() -> pollCovertArt(song.id()), 0, TimeUnit.SECONDS);
                        scheduledExecutorService.schedule(this::pollNextUp, 0, TimeUnit.SECONDS);
                    }
                });
    }

    public void pollNextUp() {
        spotifyApi.getTheUsersQueue().build().executeAsync()
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                })
                .thenAccept(playbackQueue -> {
                    List<Song> songs = new ArrayList<>();
                    for (IPlaylistItem playlistItem : playbackQueue.getQueue()) {
                        //System.out.println("    | " + playlistItem.getId() + " | \"" + playlistItem.getName() + "\" | # " + playlistItem.getHref());
                        songs.add(new Song(playlistItem.getId(), "", playlistItem.getName()));
                    }
                    //System.out.println(LocalDateTime.now());
                    nextUpCallback.accept(songs);
                });
    }

    public void pollCovertArt(String id) {
        spotifyApi.getTrack(id).build().executeAsync()
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                })
                .thenAccept(track -> {
                    try {
                        Image[] images = track.getAlbum().getImages();
                        //Arrays.stream(images).forEach(i -> System.out.println(i.getUrl() + " " + i.getWidth() + "x" + i.getHeight()));
                        coverArtCallback.accept(images.length == 0 ? null : new URL(images[0].getUrl()));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
