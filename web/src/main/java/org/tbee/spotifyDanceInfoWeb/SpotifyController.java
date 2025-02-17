package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
public class SpotifyController extends ControllerBase {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    @GetMapping("/spotify")
    public String spotify(HttpSession session, HttpServletResponse httpServletResponse, Model model) {
        try {
            updateCurrentlyPlaying(session);

            SpotifyConnectData spotifyConnectData = SpotifyConnectData.get(session);
            ScreenData screenData = ScreenData.get(session);
            screenData.showTips(LocalDateTime.now().isBefore(spotifyConnectData.connectTime().plusSeconds(10)));
            screenData.time(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

            model.addAttribute("ScreenData", screenData);
        }
        catch (Exception e) {
            logger.error("Problem constructing page", e);
            httpServletResponse.addHeader("HX-Redirect", "/");
        }
        return "spotify";
    }

    private void updateCurrentlyPlaying(HttpSession session) {
        SpotifyConnectData.get(session).newApi().getUsersCurrentlyPlayingTrack().build().executeAsync()
                .exceptionally(ControllerBase::logException)
                .thenAccept(track -> {
                    synchronized (session) {

                        ScreenData screenData = ScreenData.get(session);

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
                            setDances(session, currentlyPlaying);
                        }
                    }
                });
    }

    private void setDances(HttpSession session, Song song) {

        // The CfgSession also loaded the config.tecl file, so there is no need to look into AppCfg.
        CfgSession sessionCfg = CfgSession.get(session);
        List<String> sessionDances = sessionCfg.trackIdToDanceIds(song.trackId()).stream()
                .filter(danceId -> !danceId.isBlank())
                .map(danceId -> sessionCfg.danceIdToScreenText(danceId))
                .toList();
        song.dances(sessionDances);
    }

    private void pollArtist(HttpSession session, Song song) {
        SpotifyConnectData.get(session).newApi().getTrack(song.trackId()).build().executeAsync()
                .exceptionally(ControllerBase::logException)
                .thenAccept(t -> {
                    ArtistSimplified[] artists = t.getArtists();
                    if (artists.length > 0) {
                        String name = artists[0].getName();
                        song.artist(name);
                    }
                });
    }

    public void pollNextUp(HttpSession session, String trackId) {
        SpotifyConnectData.get(session).newApi().getTheUsersQueue().build().executeAsync()
                .exceptionally(ControllerBase::logException)
                .thenAccept(playbackQueue -> {
                    ScreenData screenData = ScreenData.get(session);
                    Song currentlyPlaying = screenData.currentlyPlaying();

                    if (currentlyPlaying != null && currentlyPlaying.trackId().equals(trackId)) {
                        List<Song> songs = new ArrayList<>();
                        for (IPlaylistItem playlistItem : playbackQueue.getQueue()) {
                            //System.out.println("    | " + playlistItem.getId() + " | \"" + playlistItem.getName() + "\" | # " + playlistItem.getHref());
                            Song song = new Song(playlistItem.getId(), playlistItem.getName(), "");
                            songs.add(song);
                            setDances(session, song);
                            if (songs.size() == 3) {
                                break;
                            }
                        }
                        screenData.nextUp(songs);

                        // Update artist
                        songs.forEach(song -> pollArtist(session, song));
                    }
                });
    }
}
