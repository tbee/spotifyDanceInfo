package org.tbee.spotifyDanceInfo;

import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

abstract public class SpotifyWebapi extends Spotify {

    public static final int EXPIRE_MARGIN = 5 * 60;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    private final Cfg cfg;

    protected final SpotifyApi spotifyApi;

    private Song currentlyPlaying = null;
    private List<Song> nextUp = null;

    public SpotifyWebapi(Cfg cfg) {
        try {
            this.cfg = cfg;

            // Setup the API
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId(cfg.webapiClientId())
                    .setClientSecret(cfg.webapiClientSecret())
                    .setRedirectUri(new URI(cfg.webapiRedirect()))
                    .build();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException("Problem connecting to Sportify webapi", e);
        }
    }

    private void currentlyPlaying(Song song) {
        currentlyPlaying = song;
        currentlyPlayingCallback.accept(song);
    }

    private void nextUp(List<Song> nextUp) {
        this.nextUp = nextUp;
        nextUpCallback.accept(Collections.unmodifiableList(nextUp));
    }

    public Spotify connect(String refreshToken) {
        try {
            spotifyApi.setRefreshToken(refreshToken);

            // Get access token
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build().execute();
            setAccessToken(authorizationCodeCredentials);

            // Start polling
            scheduledExecutorService.scheduleAtFixedRate(this::pollCurrentlyPlaying, 0, 3, TimeUnit.SECONDS);
            return this;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem connecting to Sportify webapi", e);
        }
    }

    public void pollCurrentlyPlaying() {

        spotifyApi.getUsersCurrentlyPlayingTrack().build().executeAsync()
                .exceptionally(this::logException)
                .thenAccept(track -> {
                    synchronized (SpotifyWebapi.this) {

                        boolean playing = (track != null && track.getIs_playing());
                        Song song = (!playing ? null : new Song(track.getItem().getId(), "", track.getItem().getName()));

                        // The artist changes afterward, so we cannot do an equals on the songs
                        String currentlyPlayingId = currentlyPlaying == null ? "" : currentlyPlaying.id();
                        String songId = song == null ? "" : song.id();
                        boolean songChanged = !Objects.equals(currentlyPlayingId, songId);
                        if (!songChanged) {
                            return;
                        }

                        currentlyPlaying(song);

                        if (song == null) {
                            coverArtCallback.accept(cfg.waitingImageUrl());
                            nextUp(List.of());
                        } else {
                            String id = song.id();
                            pollCovertArt(id);
                            pollNextUp(id);
                            pollArtist(id, t -> updateCurrentlyPlayingArtist(id, t));
                        }
                    }
                });
    }

    private void pollArtist(String id, Consumer<Track> callback) {
        spotifyApi.getTrack(id).build().executeAsync()
                .exceptionally(this::logException)
                .thenAccept(track -> callback.accept(track));
    }

    private void updateCurrentlyPlayingArtist(String id, Track track) {
        ifSongIsStillPlaying(id, () -> {
            ArtistSimplified[] artists = track.getArtists();
            if (artists.length == 0) {
                return;
            }
            String name = artists[0].getName();
            Song song = currentlyPlaying.withArtist(name);
            currentlyPlaying(song);
        });
    }

    public void pollNextUp(String id) {
        spotifyApi.getTheUsersQueue().build().executeAsync()
                .exceptionally(this::logException)
                .thenAccept(playbackQueue -> {
                    ifSongIsStillPlaying(id, () -> {
                        List<Song> songs = new ArrayList<>();
                        for (IPlaylistItem playlistItem : playbackQueue.getQueue()) {
                            //System.out.println("    | " + playlistItem.getId() + " | \"" + playlistItem.getName() + "\" | # " + playlistItem.getHref());
                            songs.add(new Song(playlistItem.getId(), "", playlistItem.getName()));
                        }
                        nextUp(songs);

                        // Update artist
                        songs.forEach(song -> pollArtist(song.id(), t -> updateNextUpArtist(song.id(), t)));
                    });
                });
    }

    private void updateNextUpArtist(String id, Track track) {
        ArtistSimplified[] artists = track.getArtists();
        if (artists.length == 0) {
            return;
        }
        String name = artists[0].getName();

        synchronized (SpotifyWebapi.this) {
            nextUp.stream()
                    .filter(s -> s.id().equals(id))
                    .forEach(s -> {
                        int idx = nextUp.indexOf(s);
                        nextUp.remove(idx);
                        nextUp.add(idx, s.withArtist(name));
                        nextUp(nextUp);
                    });
        }
    }

    public void pollCovertArt(String id) {
        spotifyApi.getTrack(id).build().executeAsync()
                .exceptionally(this::logException)
                .thenAccept(track -> {
                    ifSongIsStillPlaying(id, () -> {
                        try {
                            Image[] images = track.getAlbum().getImages();
                            //Arrays.stream(images).forEach(i -> System.out.println(i.getUrl() + " " + i.getWidth() + "x" + i.getHeight()));
                            coverArtCallback.accept(images.length == 0 ? null : new URL(images[0].getUrl()));
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
    }

    private <T> T logException(Throwable t) {
        t.printStackTrace();

        // just in case something went wrong with scheduled refreshing
        if (t.getMessage().contains("The access token expired")) {
            refreshAccessToken();
        }
        
        return null;
    }

    private void refreshAccessToken() {
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCodeRefresh().build().execute();
            setAccessToken(authorizationCodeCredentials);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logException(e);
        }
    }

    protected void setAccessToken(AuthorizationCodeCredentials authorizationCodeCredentials) {
        String accessToken = authorizationCodeCredentials.getAccessToken();
        spotifyApi.setAccessToken(accessToken);
        //System.out.println("accessToken " + accessToken);

        Integer expiresIn = authorizationCodeCredentials.getExpiresIn();
        scheduledExecutorService.schedule(this::refreshAccessToken, expiresIn - EXPIRE_MARGIN, TimeUnit.SECONDS);
        System.out.println("new accessToken, expires in " + expiresIn + " seconds (" + LocalDateTime.now().plusSeconds(expiresIn).withNano(0) + ")");
    }

    protected void ifSongIsStillPlaying(String id, Runnable runnable) {
        synchronized (SpotifyWebapi.this) {
            if (currentlyPlaying != null && currentlyPlaying.id().equals(id)) {
                runnable.run();
            }
        }
    }
}
