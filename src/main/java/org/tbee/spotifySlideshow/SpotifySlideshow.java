package org.tbee.spotifySlideshow;

import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.AlignY;
import net.miginfocom.layout.CC;
import org.tbee.sway.SFrame;
import org.tbee.sway.SLabel;
import org.tbee.sway.SMigPanel;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
    private SFrame sFrame;

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

        try {
            SwingUtilities.invokeAndWait(() -> {
                sImageLabel = SLabel.of();
                sTextLabel = SLabel.of();

    //            JPanel stackPanel = new JPanel(new StackLayout());
    //            stackPanel.add(sTextLabel);
    //            stackPanel.add(sImageLabel);
                SMigPanel sMigPanel = SMigPanel.of()//.debug()
                        .margin(0)
                        .noGaps()
                        .add(sImageLabel, new CC().grow().push().cell(0,0).alignX(AlignX.CENTER).alignY(AlignY.CENTER))
    //                    .add(sTextLabel, new CC().grow().push().cell(0,0))
                        ;

                sFrame = SFrame.of(sMigPanel)
                        .exitOnClose()
                        .maximize()
                        .undecorated()
                        .visible(true);
            });
        }
        catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // Load initial image
        URL waitingUrl = getClass().getResource("/waiting.jpg");
        ImageIcon waitingIcon = readAndResizeImage(waitingUrl);
        sImageLabel.setIcon(waitingIcon);

        // Start polling
        scheduledExecutorService.scheduleAtFixedRate(this::pollSpotifyAndUpdateScreen, 1, 3, TimeUnit.SECONDS);
    }

    private void pollSpotifyAndUpdateScreen() {
        try {
            TECL tecl = tecl();

            // Determine image and text
            CurrentlyPlaying currentlyPlaying = spotify.getUsersCurrentlyPlayingTrack();
            String dance = "undefined";
            String undefinedImage = getClass().getResource("/undefined.jpg").toExternalForm();
            String image = undefinedImage;
            String text = "-";
            if (currentlyPlaying == null || !currentlyPlaying.getIs_playing()) {
                image = getClass().getResource("/waiting.jpg").toExternalForm();
            }
            else {
                IPlaylistItem item = currentlyPlaying.getItem();
                String trackId = item.getId();
                dance = tecl.grp("/tracks").str("id", trackId, "dance", "undefined");
                image = tecl.grp("/dances").str("id", dance, "image", undefinedImage);
                text = tecl.grp("/dances").str("id", dance, "text", "-");

//                System.out.println("getCurrentlyPlayingType " + currentlyPlaying.getCurrentlyPlayingType());
                System.out.println("| " + trackId + " | " + dance + " | # " + item.getName() + " / " + item.getExternalUrls().get("spotify"));
            }

            // Load image
            URI uri = new URI("-".equals(image) ? undefinedImage : image);
            int contentLength = uri.toURL().openConnection().getContentLength();
            if (contentLength == 0) {
                throw new RuntimeException("Image not found " + uri);
            }
            ImageIcon icon = readAndResizeImage(uri.toURL());

            // Update screen
            String textFinal = text;
            SwingUtilities.invokeLater(() -> {
                sTextLabel.visible(!"-".equals(textFinal));
                sTextLabel.setText(textFinal);
                sImageLabel.setIcon(icon);
            });
        }
        catch (RuntimeException | URISyntaxException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ImageIcon readAndResizeImage(URL url) {
        try {
            BufferedImage originalImage = ImageIO.read(url);

            Dimension sFrameSize = sFrame.getSize();
            int width = (int) sFrameSize.getWidth();
            int height = (int) sFrameSize.getHeight();
            BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = resizedImg.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(originalImage, 0, 0, width, height, null);
            g2.dispose();

            return new ImageIcon(resizedImg);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}