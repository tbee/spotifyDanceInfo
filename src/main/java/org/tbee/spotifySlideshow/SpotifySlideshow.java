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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
            String undefinedImage = getClass().getResource("/undefined.jpg").toExternalForm();
            String image = undefinedImage;
            String text = "-";
            if (currentlyPlaying == null) {
                image = getClass().getResource("/waiting.jpg").toExternalForm();
            }
            else {
                String trackId = currentlyPlaying.getItem().getId();
                dance = tecl.grp("/tracks").str("id", trackId, "dance", "undefined");
                image = tecl.grp("/dances").str("id", dance, "image", undefinedImage);
                text = tecl.grp("/dances").str("id", dance, "text", "-");

                System.out.println("getIs_playing " + currentlyPlaying.getIs_playing());
                System.out.println("getCurrentlyPlayingType " + currentlyPlaying.getCurrentlyPlayingType());
                System.out.println("getId " + trackId);
                System.out.println("getName " + currentlyPlaying.getItem().getName());
                System.out.println("dance " + dance);
                System.out.println("image " + image);
            }

            String textFinal = text;
            String imageFinal = image;

            URI url = new URI("-".equals(imageFinal) ? undefinedImage : image);
            ImageIcon icon = new ImageIcon(url.toURL());
            SwingUtilities.invokeLater(() -> {
                sTextLabel.visible(!"-".equals(textFinal));
                sTextLabel.setText(textFinal);

                sImageLabel.visible(!"-".equals(imageFinal));
                sImageLabel.setIcon(icon);
            });
        }
        catch (RuntimeException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}