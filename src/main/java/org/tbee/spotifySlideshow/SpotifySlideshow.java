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
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

    // Remember last settings to be able to refresh
    private boolean playing = false;
    private Song song = null;
    private String logline = "";

    public static void main(String[] args) {
        new SpotifySlideshow().run();
    }

    public static TECL tecl() {
        try {
            TECL tecl = TECL.parser().findAndParse();
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
                sFrame.addPropertyChangeListener("graphicsConfiguration", e -> updateScreen());
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
        String CONNECT_LOCAL = "local";
        String connect = tecl().str("/spotify/connect", CONNECT_LOCAL);
        if (CONNECT_LOCAL.equalsIgnoreCase(connect)) {
            startSpotifyLocalApi();
        }
        else {
            startSpotifyWebapi();
        }
    }

    private void startSpotifyWebapi() {
        // Connect to spotify
        spotifyWebapi = new SpotifyWebapi(tecl().bool("/webapi/simulate", false));
        spotifyWebapi.connect();

        // Start polling
        scheduledExecutorService.scheduleAtFixedRate(this::pollSpotifyWebapiAndUpdateScreen, 0, 3, TimeUnit.SECONDS);
    }

    private void pollSpotifyWebapiAndUpdateScreen() {
        try {
            CurrentlyPlaying currentlyPlaying = spotifyWebapi.getUsersCurrentlyPlayingTrack();
            if (currentlyPlaying == null || !currentlyPlaying.getIs_playing()) {
                updateScreen(false, null);
            }
            else {
                IPlaylistItem item = currentlyPlaying.getItem();
                updateScreen(true, new Song(item.getId(), "", item.getName()));
            }
        }
        catch (RuntimeException e) {
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
                updateScreen(true, new Song(track.getId(), track.getArtist(), track.getName()));
            }

            @Override
            public void onPositionChanged(int position) { }

            @Override
            public void onPlayBackChanged(boolean isPlaying) {
                updateScreen(isPlaying, song);
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

    private void updateScreen() {
        updateScreen(playing, song);
    }

    private void updateScreen(boolean playing, Song song) {
        this.playing = playing;
        this.song = song;

        try {
            TECL tecl = tecl();
            String undefinedImage = getClass().getResource("/undefined.jpg").toExternalForm();

            // Determine image and text
            String image;
            String text;
            String logline;
            if (!playing) {
                image = getClass().getResource("/waiting.jpg").toExternalForm();
                text = "";
                logline = "Nothing is playing";
            }
            else {
                String trackId = song.id();
                String dance = tecl.grp("/tracks").str("id", trackId, "dance", "undefined");
                image = tecl.grp("/dances").str("id", dance, "image", undefinedImage);
                text = tecl.grp("/dances").str("id", dance, "text", "<div>" + song.artist() + "</div><div>" + song.name() + "</div>");
                logline = "| " + trackId + " | " + (dance + "                    ").substring(0, 20) + " | # " + (song.artist().isBlank() ? "" : song.artist() + " - ") + song.name() + " / https://open.spotify.com/track/" + trackId;
            }
            if (!logline.equals(this.logline)) {
                System.out.println(logline);
                this.logline = logline;
            }

            // Load image
            URI uri = new URI(image);
            int contentLength = uri.toURL().openConnection().getContentLength();
            if (contentLength == 0) {
                System.out.println("Image not found " + uri);
                uri = new URI(undefinedImage);
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