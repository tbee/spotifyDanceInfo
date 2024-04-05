package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.tbee.spotifyDanceInfo.Cfg;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
public class SpotifyController {

    @GetMapping("/spotify")
    public String spotify(HttpSession session, Model model) {
        updateCurrentlyPlaying(session);
        model.addAttribute("ScreenData", screenData(session));
        return "spotify";
    }

    private void updateCurrentlyPlaying(HttpSession session) {
        ConnectController.spotifyApi(session).getUsersCurrentlyPlayingTrack().build().executeAsync()
                .exceptionally(t -> logException(session, t))
                .thenAccept(track -> {
                    synchronized (session) {

                        ScreenData screenData = screenData(session);

                        boolean playing = (track != null && track.getIs_playing());
                        Song currentlyPlaying = !playing ? new Song() : new Song(track.getItem().getId(), track.getItem().getName(), "");

                        // The artist changes afterward, so we cannot do an equals on the songs
                        boolean songChanged = !Objects.equals(currentlyPlaying.trackId(), screenData.currentlyPlaying().trackId());
                        if (!songChanged) {
                            return;
                        }

                        screenData.currentlyPlaying(currentlyPlaying);
                        if (currentlyPlaying.trackId().isBlank()) {
                            //coverArtCallback.accept(cfg.waitingImageUrl());
                            screenData.nextUp(List.of());
                        }
                        else {
                            //pollCovertArt(id);
                            pollArtist(session, currentlyPlaying);
                            pollNextUp(session, currentlyPlaying.trackId());
                            setDances(currentlyPlaying);
                        }
                    }
                });
    }

    private void setDances(Song song) {
        Cfg cfg = SpotifyDanceInfoWebApplication.cfg();
        List<String> dances = cfg.trackIdToDanceIds(song.trackId()).stream()
                .map(danceId -> cfg.danceIdToScreenText(danceId))
                .toList();
        song.dances(dances);
    }

    private void pollArtist(HttpSession session, Song song) {
        ConnectController.spotifyApi(session).getTrack(song.trackId()).build().executeAsync()
                .exceptionally(t -> logException(session, t))
                .thenAccept(t -> {
                    ArtistSimplified[] artists = t.getArtists();
                    if (artists.length > 0) {
                        String name = artists[0].getName();
                        song.artist(name);
                    }
                });
    }

    public void pollNextUp(HttpSession session, String trackId) {
        ConnectController.spotifyApi(session).getTheUsersQueue().build().executeAsync()
                .exceptionally(t -> logException(session, t))
                .thenAccept(playbackQueue -> {
                    ScreenData screenData = screenData(session);
                    Song currentlyPlaying = screenData.currentlyPlaying();

                    if (currentlyPlaying != null && currentlyPlaying.trackId().equals(trackId)) {
                        List<Song> songs = new ArrayList<>();
                        for (IPlaylistItem playlistItem : playbackQueue.getQueue()) {
                            //System.out.println("    | " + playlistItem.getId() + " | \"" + playlistItem.getName() + "\" | # " + playlistItem.getHref());
                            Song song = new Song(playlistItem.getId(), playlistItem.getName(), "");
                            songs.add(song);
                            if (songs.size() == 3) {
                                break; // TBEERNOT
                            }
                            setDances(song);
                        }
                        screenData.nextUp(songs);

                        // Update artist
                        songs.forEach(song -> pollArtist(session, song));
                    }
                });
    }


    private static SpotifyConnectData spotifyConnectData(HttpSession session) {
        String attributeName = "SpotifyConnectData";
        SpotifyConnectData spotifyConnectData = (SpotifyConnectData) session.getAttribute(attributeName);
        if (spotifyConnectData == null) {
            spotifyConnectData = new SpotifyConnectData();
            session.setAttribute(attributeName, spotifyConnectData);
        }
        return spotifyConnectData;
    }

    private static ScreenData screenData(HttpSession session) {
        String attributeName = "SpotifyScreenData";
        ScreenData screenData = (ScreenData) session.getAttribute(attributeName);
        if (screenData == null) {
            screenData = new ScreenData();
            session.setAttribute(attributeName, screenData);
        }
        return screenData;
    }

    private <T> T logException(HttpSession session, Throwable t) {
        t.printStackTrace();

        // just in case something went wrong with scheduled refreshing
        if (t.getMessage().contains("The access token expired")) {
            refreshAccessToken(session);
        }

        return null;
    }

    private void refreshAccessToken(HttpSession session) {
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = ConnectController.spotifyApi(session).authorizationCodeRefresh().build().execute();
            SpotifyConnectData spotifyConnectData = spotifyConnectData(session);
            spotifyConnectData
                    .refreshToken(authorizationCodeCredentials.getRefreshToken() != null ? authorizationCodeCredentials.getRefreshToken() : spotifyConnectData.refreshToken())
                    .accessToken(authorizationCodeCredentials.getAccessToken());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logException(session, e);
        }
    }
}