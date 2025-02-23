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
import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Controller
public class SpotifyController extends ControllerBase {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    private static final String SCHEDULED_FUTURES = "scheduledFutures";
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    @GetMapping("/spotify")
    public String spotify(HttpSession session, HttpServletResponse httpServletResponse, Model model) {
        try {
            // On the first time, setup the polling administration
            if (session.getAttribute(SpotifyController.SCHEDULED_FUTURES) == null) {
                session.setAttribute(SpotifyController.SCHEDULED_FUTURES, new ArrayList<ScheduledFuture<?>>());
            }
            // Start polling
            List<ScheduledFuture<?>> scheduledFutures = (List<ScheduledFuture<?>>) session.getAttribute(SpotifyController.SCHEDULED_FUTURES);
            if (scheduledFutures.isEmpty()) {

                // Remember the session weakly, so this does not lock it, for cleanup if it gets disposed by the container
                WeakReference<HttpSession> sessionWeakReference = new WeakReference<>(session);
                scheduledFutures.add(scheduledExecutorService.scheduleAtFixedRate(() -> {

                    // If the session was disposed by the container, stop polling
                    HttpSession httpSession = sessionWeakReference.get();
                    if (httpSession == null) {
                        if (logger.isInfoEnabled()) logger.info("session is null, aborting all {} associated scheduled tasks", scheduledFutures.size());
                        scheduledFutures.forEach(sf -> sf.cancel(true));
                        scheduledFutures.clear();
                        return;
                    }

                    // Poll
                    updateCurrentlyPlaying(httpSession);
                }, 0, 5, TimeUnit.SECONDS));
            }

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
        try {
            CfgSession.get(session).rateLimiterCurrentlyPlaying().claim("CurrentlyPlaying");
            CurrentlyPlaying currentlyPlaying = SpotifyConnectData.get(session).newApi().getUsersCurrentlyPlayingTrack().build().execute();
            ScreenData screenData = ScreenData.get(session);

            boolean playing = (currentlyPlaying != null && currentlyPlaying.getIs_playing());
            Song song = !playing ? new Song() : new Song(currentlyPlaying.getItem().getId(), currentlyPlaying.getItem().getName(), "");

            // The artist changes afterward, so we cannot do an equals on the songs
            boolean songChanged = !Objects.equals(song.trackId(), screenData.currentlyPlaying().trackId());
            if (!songChanged) {
                return;
            }

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
