package org.tbee.spotifySlideshow;

import net.miginfocom.layout.CC;
import org.tbee.sway.SFrame;
import org.tbee.sway.SLabel;
import org.tbee.sway.SMigPanel;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpotifySlideshow {

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private Spotify spotify;
    private SLabel sImageLabel;
    private SLabel sTextLabel;

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

        spotify = new Spotify(true);
        spotify.connect();

        URL waitingUrl = getClass().getResource("/waiting.jpg");
        ImageIcon waitingIcon = new ImageIcon(waitingUrl);
        sImageLabel = SLabel.of(waitingIcon);
        sTextLabel = SLabel.of("Noting is playing");
        SwingUtilities.invokeLater(() -> {
            SMigPanel sMigPanel = SMigPanel.of().debug()
                    .margin(0)
                    .noGaps()
                    .add(sImageLabel, new CC().grow().push().cell(0,0))
                    .add(sTextLabel, new CC().grow().push().cell(0,0))
                    ;
            SFrame.of(sMigPanel)
                    .exitOnClose()
                    .maximize()
                    .undecorated()
                    .visible(true);
        });

        scheduledExecutorService.scheduleAtFixedRate(this::generateEvents, 1, 3, TimeUnit.SECONDS);
    }

    private void generateEvents() {
        try {
            TECL tecl = tecl();
            CurrentlyPlaying currentlyPlaying = spotify.getUsersCurrentlyPlayingTrack();
            String dance = "undefined";
            String image = getClass().getResource("/undefined.jpg").toExternalForm();
            if (currentlyPlaying == null) {
                image = getClass().getResource("/waiting.jpg").toExternalForm();
            }
            else {
                String trackId = currentlyPlaying.getItem().getId();
                try {
                    List<String> trackIds = tecl.list("/tracks/id", Collections.emptyList(), String.class);
                    List<String> trackDances = tecl.list("/tracks/dance", Collections.emptyList(), String.class);
                    int trackIndex = trackIds.indexOf(trackId);
                    dance = trackDances.get(trackIndex);

                    try {
                        List<String> danceIds = tecl.list("/dances/id", Collections.emptyList(), String.class);
                        List<String> danceImages = tecl.list("/dances/image", Collections.emptyList(), String.class);
                        int danceIndex = danceIds.indexOf(dance);
                        image = danceImages.get(danceIndex);
                    }
                    catch (RuntimeException e) {
                        System.err.println("Could not find image for dance '" + dance + "'");
                    }
                }
                catch (RuntimeException e) {
                    System.err.println("Could not find track id " + trackId);
                }

                System.out.println("getIs_playing " + currentlyPlaying.getIs_playing());
                System.out.println("getCurrentlyPlayingType " + currentlyPlaying.getCurrentlyPlayingType());
                System.out.println("getId " + trackId);
                System.out.println("getName " + currentlyPlaying.getItem().getName());
                System.out.println("dance " + dance);
                System.out.println("image " + image);
            }

            SwingUtilities.invokeLater(() -> sTextLabel.setText(currentlyPlaying.getItem().getName()));
            URL url = new URL(image);
            ImageIcon icon = new ImageIcon(url);
            SwingUtilities.invokeLater(() -> sImageLabel.setIcon(icon));
        }
        catch (RuntimeException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}