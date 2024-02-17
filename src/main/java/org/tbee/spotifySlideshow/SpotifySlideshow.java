package org.tbee.spotifySlideshow;

import org.tbee.sway.SFrame;
import org.tbee.sway.SLabel;
import org.tbee.sway.SMigPanel;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpotifySlideshow {

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private Spotify spotify;
    private SLabel sLabel;

    public static void main(String[] args) {
        new SpotifySlideshow().run();
    }

    public static TECL tecl() {
        try {
            TECL tecl = TECL.parser().findAndParse("spotifySlideshow.tecl");
            return tecl;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() {

        spotify = new Spotify(false);
        spotify.connect();

        sLabel = SLabel.of("Waiting for track data...");
        SwingUtilities.invokeLater(() -> {
            SMigPanel sMigPanel = SMigPanel.of(sLabel);
            SFrame.of(sMigPanel)
                    .exitOnClose()
                    .maximize()
                    .undecorated()
                    .visible(true);
        });

        scheduledExecutorService.scheduleAtFixedRate(this::generateEvents, 5, 5, TimeUnit.SECONDS);
    }

    private void generateEvents() {
        CurrentlyPlaying currentlyPlaying = spotify.getUsersCurrentlyPlayingTrack();
        if (currentlyPlaying != null) {
            System.out.println("getIs_playing " + currentlyPlaying.getIs_playing());
            System.out.println("getCurrentlyPlayingType " + currentlyPlaying.getCurrentlyPlayingType());
            System.out.println("getId " + currentlyPlaying.getItem().getId());
            System.out.println("getName " + currentlyPlaying.getItem().getName());
            SwingUtilities.invokeLater(() -> sLabel.setText(currentlyPlaying.getItem().getName()));
        }
    }
}