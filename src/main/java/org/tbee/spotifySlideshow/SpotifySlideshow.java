package org.tbee.spotifySlideshow;

import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.Track;
import org.jdesktop.swingx.StackLayout;
import org.tbee.sway.SFrame;
import org.tbee.sway.SLabel;
import org.tbee.sway.SLookAndFeel;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
    private SpotifyWebapi spotifyWebapi;
    private SpotifyAPI spotifyLocalApi;

    private SLabel sImageLabel;
    private SLabel sTextLabel;
    private SLabel sTextLabelShadow;
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
        SLookAndFeel.installDefault();

        try {
            SwingUtilities.invokeAndWait(() -> {
                sImageLabel = SLabel.of();

                sTextLabel = SLabel.of();
                sTextLabel.setVerticalAlignment(SwingConstants.BOTTOM);
                sTextLabel.setHorizontalAlignment(SwingConstants.CENTER);
                sTextLabel.setForeground(Color.WHITE);
                sTextLabel.setFont(new Font("Verdana", Font.PLAIN, 80));

                sTextLabelShadow = SLabel.of();
                sTextLabelShadow.setForeground(Color.BLACK);
                sTextLabelShadow.setBorder(BorderFactory.createEmptyBorder(0, 5, 3, 0)); // create a small offset
                sTextLabelShadow.setVerticalAlignment(sTextLabel.getVerticalAlignment());
                sTextLabelShadow.setHorizontalAlignment(sTextLabel.getHorizontalAlignment());
                sTextLabelShadow.setFont(sTextLabel.getFont());

                JPanel stackPanel = new JPanel(new StackLayout());
                stackPanel.add(sImageLabel);
                stackPanel.add(sTextLabelShadow);
                stackPanel.add(sTextLabel);

                sFrame = SFrame.of(stackPanel)
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

        // And go
        startSpotifyLocalApi();
        //startSpotifyWebapi();
    }

    private void startSpotifyWebapi() {
        // Connect to spotify
        spotifyWebapi = new SpotifyWebapi(tecl().bool("/spotify/simulate", false));
        spotifyWebapi.connect();

        // Start polling
        scheduledExecutorService.scheduleAtFixedRate(this::pollSpotifyWebapiAndUpdateScreen, 1, 3, TimeUnit.SECONDS);
    }

    private void pollSpotifyWebapiAndUpdateScreen() {
        try {
            TECL tecl = tecl();

            // Determine image and text
            CurrentlyPlaying currentlyPlaying = spotifyWebapi.getUsersCurrentlyPlayingTrack();
            String dance = "undefined";
            String undefinedImage = getClass().getResource("/undefined.jpg").toExternalForm();
            String image = undefinedImage;
            String text = "";
            if (currentlyPlaying == null || !currentlyPlaying.getIs_playing()) {
                image = getClass().getResource("/waiting.jpg").toExternalForm();
                System.out.println("Nothing is playing");
            }
            else {
                IPlaylistItem item = currentlyPlaying.getItem();
                String trackId = item.getId();
                dance = tecl.grp("/tracks").str("id", trackId, "dance", "undefined");
                image = tecl.grp("/dances").str("id", dance, "image", undefinedImage);
                text = tecl.grp("/dances").str("id", dance, "text", item.getName());
                System.out.println("| " + trackId + " | " + dance + " | # " + item.getName() + " / " + item.getExternalUrls().get("spotify"));
            }

            // Load image
            URI uri = new URI(image.isBlank() ? undefinedImage : image);
            int contentLength = uri.toURL().openConnection().getContentLength();
            if (contentLength == 0) {
                throw new RuntimeException("Image not found " + uri);
            }
            ImageIcon icon = readAndResizeImage(uri.toURL());

            // Update screen
            String textFinal = text;
            SwingUtilities.invokeLater(() -> {
                sImageLabel.setIcon(icon);

                sTextLabel.setText("<html><body>" + textFinal + "</body></html>");
                sTextLabelShadow.setText(sTextLabel.getText());
            });
        }
        catch (RuntimeException | URISyntaxException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startSpotifyLocalApi() {
        spotifyLocalApi = SpotifyAPIFactory.create();
        spotifyLocalApi.registerListener(new SpotifyListener() {
            @Override
            public void onConnect() {
            }

            @Override
            public void onTrackChanged(Track track) {
                updateScreenFromSpotifyLocalApi();
            }

            @Override
            public void onPositionChanged(int position) { }

            @Override
            public void onPlayBackChanged(boolean isPlaying) {
                updateScreenFromSpotifyLocalApi();
            }

            @Override
            public void onSync() { }

            @Override
            public void onDisconnect(Exception exception) {
                exception.printStackTrace();
                spotifyLocalApi.stop();
            }
        });
        spotifyLocalApi.initialize();
    }

    private void updateScreenFromSpotifyLocalApi() {
        try {
            TECL tecl = tecl();

            // Determine image and text
            String dance = "undefined";
            String undefinedImage = getClass().getResource("/undefined.jpg").toExternalForm();
            String image = undefinedImage;
            String text = "";
            if (!spotifyLocalApi.hasTrack()) {
                image = getClass().getResource("/waiting.jpg").toExternalForm();
                System.out.println("Nothing is playing");
            }
            else {
                Track track = spotifyLocalApi.getTrack();
                String trackId = track.getId();
                dance = tecl.grp("/tracks").str("id", trackId, "dance", "undefined");
                image = tecl.grp("/dances").str("id", dance, "image", undefinedImage);
                text = tecl.grp("/dances").str("id", dance, "text", "<div>" + track.getArtist() + "</div><div>" + track.getName() + "</div>");
                System.out.println("| " + trackId + " | " + dance + " | # " + track.getArtist() + " - " + track.getName());
            }

            // Load image
            URI uri = new URI(image.isBlank() ? undefinedImage : image);
            int contentLength = uri.toURL().openConnection().getContentLength();
            if (contentLength == 0) {
                throw new RuntimeException("Image not found " + uri);
            }
            ImageIcon icon = readAndResizeImage(uri.toURL());

            // Update screen
            String textFinal = text;
            SwingUtilities.invokeLater(() -> {
                sImageLabel.setIcon(icon);

                sTextLabel.setText("<html><body>" + textFinal + "</body></html>");
                sTextLabelShadow.setText(sTextLabel.getText());
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
            BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = resizedImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(originalImage, 0, 0, width, height, null);
            g2.dispose();

            return new ImageIcon(resizedImage);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}