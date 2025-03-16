package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class SpotifyController extends ControllerBase {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    private static final ExecutorService executorService = Executors.newFixedThreadPool(3); // newCachedThreadPool();

    @GetMapping("/spotify")
    public String spotify(HttpSession session, HttpServletResponse httpServletResponse, Model model) {
        setVersion(model);
        SpotifyConnectData spotifyConnectData = SpotifyConnectData.get(session);
        if (spotifyConnectData == null) {
            return "redirect:/";
        }
        ScreenData screenData = ScreenData.get(session);
        screenData.showTips(LocalDateTime.now().isBefore(spotifyConnectData.connectTime().plusSeconds(10)));
        screenData.time(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        model.addAttribute("ScreenData", screenData);

        // Poll the current song
        AtomicInteger counter = spotifyConnectData.counter();
        if (counter.get() > 0) {
            if (logger.isDebugEnabled()) logger.debug("Polling updateCurrentlyPlaying already in progress");
        }
        else {
            counter.incrementAndGet();
            if (logger.isDebugEnabled()) logger.debug("Polling updateCurrentlyPlaying scheduled");
            executorService.execute(() -> {
                try {
                    if (logger.isDebugEnabled()) logger.debug("Polling updateCurrentlyPlaying");
                    updateCurrentlyPlaying(session);
                }
                catch (RuntimeException e) {
                    logger.error("Exception in the updateCurrentlyPlaying polling", e);
                }
                finally {
                    if (logger.isDebugEnabled()) logger.debug("Polling updateCurrentlyPlaying complete");
                    counter.decrementAndGet();
                }
            });
        }

        return "spotify";
    }

    private void updateCurrentlyPlaying(HttpSession session) {
        try {
            if (logger.isDebugEnabled()) logger.debug("accessToken: using " + SpotifyConnectData.get(session).accessToken());
            CfgSession.get(session).rateLimiterCurrentlyPlaying().claim("CurrentlyPlaying");
            CurrentlyPlaying currentlyPlaying = SpotifyConnectData.get(session).newApi().getUsersCurrentlyPlayingTrack().build().execute();
            ScreenData screenData = ScreenData.get(session);

            // Create the candidate song object
            boolean playing = (currentlyPlaying != null && currentlyPlaying.getIs_playing());
            Song song = !playing ? new Song() : new Song(currentlyPlaying.getItem().getId(), currentlyPlaying.getItem().getName(), "");

            // The artist is updated async, so we cannot do an equals on the songs, only the track id
            boolean songChanged = !Objects.equals(song.trackId(), screenData.currentlyPlaying().trackId());
            if (!songChanged && !screenData.forceRefresh()) {
                return;
            }
            screenData.forceRefresh(false); // clear the flag

            screenData.currentlyPlaying(song);
            if (song.trackId().isBlank()) {
                //coverArtCallback.accept(cfg.waitingImageUrl());
                screenData.nextUp(List.of());
            }
            else {
                //pollCovertArt(id);
                pollArtist(session, song);
                pollNextUp(session, song.trackId());
                setDances(session, song);
            }
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Problem updating currently playing", e);
            // this is the place where we detect expired sessions
            if (e.getMessage().contains("The access token expired")) {
                SpotifyConnectData.get(session).refreshAccessToken(session);
            }
            throw new RuntimeException(e);
        }
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
        CfgSession.get(session).rateLimiterCurrentlyPlaying().claim("getTrack");
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
        CfgSession.get(session).rateLimiterCurrentlyPlaying().claim("getTheUsersQueue");
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
