package org.tbee.spotifyDanceInfoQrks;

import groovy.util.logging.Log;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.spotifyDanceInfo.Cfg;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log
@Path("")
@Produces(MediaType.TEXT_HTML)
public class SpotifyHtmlResource extends ResourceBase {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyHtmlResource.class);

    @Inject
    Thymeleaf thymeleaf;

    @GET
    @Path("/spotify")
    public Object spotify(@Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        try {
            HttpSession session = httpServletRequest.getSession();
            updateCurrentlyPlaying(session);

            SpotifyConnectData spotifyConnectData = spotifyConnectData(session);
            ScreenData screenData = screenData(session);
            screenData.showTips(LocalDateTime.now().isBefore(spotifyConnectData.connectTime().plusSeconds(10)));
            screenData.time(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

            return thymeleaf.template("spotify")
                    .with("ScreenData", screenData);
        }
        catch (Exception e) {
            logger.error("Problem constructing page", e);
            httpServletResponse.addHeader("HX-Redirect", "/");
        }
        try {
            return Response.status(Response.Status.FOUND).location(new URI("/")).build(); // redirect not working
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateCurrentlyPlaying(HttpSession session) {
        spotifyApi(session).getUsersCurrentlyPlayingTrack().build().executeAsync()
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
                            setDances(session, currentlyPlaying);
                        }
                    }
                });
    }

    private void setDances(HttpSession session, Song song) {

        // First check in the session config
        Cfg sessionCfg = (Cfg)session.getAttribute("cfg");
        List<String> sessionDances = sessionCfg.trackIdToDanceIds(song.trackId()).stream()
                .filter(danceId -> !danceId.isBlank())
                .map(danceId -> sessionCfg.danceIdToScreenText(danceId))
                .toList();
        if (!sessionDances.isEmpty()) {
            song.dances(sessionDances);
            return;
        }

        // Then in the application config
        Cfg applicationCfg = SpotifyDanceInfoQrks.cfg();
        List<String> applicationDances = applicationCfg.trackIdToDanceIds(song.trackId()).stream()
                .map(danceId -> applicationCfg.danceIdToScreenText(danceId))
                .toList();
        song.dances(applicationDances);
    }

    private void pollArtist(HttpSession session, Song song) {
        spotifyApi(session).getTrack(song.trackId()).build().executeAsync()
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
        spotifyApi(session).getTheUsersQueue().build().executeAsync()
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
                            setDances(session, song);
                        }
                        screenData.nextUp(songs);

                        // Update artist
                        songs.forEach(song -> pollArtist(session, song));
                    }
                });
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
}