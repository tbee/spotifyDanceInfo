package org.tbee.spotifyDanceInfo;

import org.tbee.sway.SContextMenu;
import org.tbee.sway.SFrame;
import org.tbee.sway.SLabel;
import org.tbee.sway.SLookAndFeel;
import org.tbee.sway.SOptionPane;
import org.tbee.sway.SStackedPanel;
import org.tbee.sway.support.HAlign;
import org.tbee.sway.support.VAlign;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class SpotifyDanceInfo {

    public static URL WAITING_IMAGE_URL;
    public static URL BACKGROUND_IMAGE_URL;

    private Cfg cfg;
    // Screen
    private SLabel sImageLabel;
    private SLabel sTextLabel;
    private SLabel sNextTextLabel;
    private SFrame sFrame;

    // Current state
    private Song song = null;
    private List<Song> nextUpSongs = List.of();
    private URL covertArtUrl;

    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        System.out.print("Available fonts: ");
        Arrays.stream(ge.getAvailableFontFamilyNames()).forEach(f -> System.out.print(f + ", "));
        System.out.println();

        new SpotifyDanceInfo().run();
    }

    private void run() {
        SLookAndFeel.installDefault();
        SContextMenu.install();
        Cfg cfg = cfg();

        try {
            WAITING_IMAGE_URL = cfg.waitingImageUrl();
            BACKGROUND_IMAGE_URL = cfg.backgroundImageUrl();

            SwingUtilities.invokeAndWait(() -> {
                sImageLabel = SLabel.of();

                Font songFont = cfg.songFont();
                System.out.println("Using songFont "+ songFont.getFontName() + " " + songFont.getSize());
                sTextLabel = ShadowLabel.of()
                        .vAlign(VAlign.TOP)
                        .hAlign(HAlign.LEFT)
                        .margin(10, 10, 0, 0)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY)
                        .font(songFont);

                Font nextFont = cfg.nextFont();
                System.out.println("Using nextfont "+ nextFont.getFontName() + " " + nextFont.getSize());
                sNextTextLabel = ShadowLabel.of()
                        .vAlign(VAlign.BOTTOM)
                        .hAlign(HAlign.RIGHT)
                        .margin(0, 0, 10, 10)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY)
                        .font(nextFont);

                SStackedPanel stackPanel = SStackedPanel.of(sImageLabel, sNextTextLabel, sTextLabel);

                sFrame = SFrame.of(stackPanel)
                        .exitOnClose()
                        .maximize()
                        .undecorated()
                        .title("Spotify Dance Info")
                        .iconImage(readImage(getClass().getResource("/icon.png")))
                        .onKeyTyped(this::reactToKeyPress)
                        .onPropertyChange("graphicsConfiguration", e -> updateCurrentlyPlaying())
                        .visible(true);
            });
        }
        catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // Load initial image
        ImageIcon waitingIcon = readAndResizeImageFilling(WAITING_IMAGE_URL);
        sImageLabel.setIcon(waitingIcon);

        // And go
        (cfg.connectLocal() ? new SpotifyLocalApi() : new SpotifyWebapi(cfg))
                .currentlyPlayingCallback(this::updateCurrentlyPlaying)
                .nextUpCallback(this::updateNextUp)
                .coverArtCallback(this::generateAndUpdateImage)
                .connect();
    }

    private void reactToKeyPress(KeyEvent e) {
        if (e.getKeyChar() == 'r') {
            cfg = null; // force reload
            updateAll();
        }
        else if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
        else if (e.getKeyChar() == ' ') {
            SOptionPane.ofInfo(sFrame, "Supported keys",
                        """
                        [esc] = Quit the application
                        [r] = Reload configuration, but does not reconnect (if that was changed in the configuration).
                        """);
        }
    }

    private void updateAll() {
        updateCurrentlyPlaying();
        updateNextUp();
        generateAndUpdateImage();
    }

    private void generateAndUpdateImage(URL url) {
        this.covertArtUrl = url;
        generateAndUpdateImage();
    }

    private void generateAndUpdateImage() {
        if (covertArtUrl == null || !cfg().useCoverArt()) {
            this.sImageLabel.setIcon(readAndResizeImageFilling(song == null ? WAITING_IMAGE_URL : BACKGROUND_IMAGE_URL));
            return;
        }

        BufferedImage image = readImage(covertArtUrl);

        Dimension frameSize = sFrame.getSize();
        BufferedImage resizedFillingImage = ImageUtil.resizeFilling(image, frameSize);
        BufferedImage resizedFittingImage = ImageUtil.resizeFitting(image, frameSize);

        ImageUtil.addNoise(40.0, resizedFillingImage);

        Graphics2D g2 = resizedFillingImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int centeredX = (resizedFillingImage.getWidth() - resizedFittingImage.getWidth()) / 2;
        int centeredY = (resizedFillingImage.getHeight() - resizedFittingImage.getHeight()) / 2;
        g2.drawImage(resizedFittingImage, centeredX, centeredY, null);
        g2.dispose();

        ImageUtil.addNoise(cfg.backgroundImageNoise(), resizedFillingImage);

        this.sImageLabel.setIcon(new ImageIcon(resizedFillingImage));
    }

    private void updateCurrentlyPlaying(Song song) {
        this.song = song;
        updateCurrentlyPlaying();
    }

    private void updateCurrentlyPlaying() {

        try {
            Cfg cfg = cfg();

            // Determine image and text
            final StringBuilder text = new StringBuilder();
            String logline;
            if (this.song == null) {
                logline = "Nothing is playing";
            }
            else {
                String trackId = song.id();
                List<String> danceIds = cfg.trackIdToDanceIds(trackId);
                text.append(song.artist().isBlank() ? "" : song.artist() + "<br>")
                    .append(song.name())
                    .append("<br>");
                for (String danceId : danceIds) {
                    String screenText = cfg.danceIdToScreenText(danceId);
                    text.append(screenText.isBlank() ? "" : "<br>" + screenText);
                }
                logline = logline(song, danceIds.isEmpty() ? "" : danceIds.getFirst());

                if (cfg.copyTrackLoglineToClipboard()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(logline), null);
                }
            }
            System.out.println(logline);

            // Update screen
            SwingUtilities.invokeLater(() -> {
                sTextLabel.setText("<html><body><div style=\"text-align:left;\">" + text.toString() + "</div></body></html>");
            });
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateNextUp(List<Song> songs) {
        this.nextUpSongs = songs;
        updateNextUp();
    }

    private void updateNextUp() {

        try {
            Cfg cfg = cfg();

            int count = cfg.nextUpCount();
            List<Song> songs = nextUpSongs.subList(0, Math.min(nextUpSongs.size(), count));

            // Determine text
            StringBuilder text = new StringBuilder();
            songs.forEach(nextSong -> {
                String trackId = nextSong.id();
                text.append("<br><br>")
                    .append(nextSong.artist().isBlank() ? "" : nextSong.artist() + " - ")
                    .append(nextSong.name());
                cfg.trackIdToDanceIds(trackId).stream()
                    .filter(dance -> dance != null && !dance.isBlank())
                    .forEach(danceId -> {
                        text.append("<br>")
                            .append(cfg.danceIdToScreenText(danceId));
                    });
            });

            // Update screen
            SwingUtilities.invokeLater(() -> {
                sNextTextLabel.setText("<html><body><div style=\"text-align:right;\">" + (text.isEmpty() ? "" : "Next up:") + text.toString() + "</div></body></html>");
            });
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ImageIcon readAndResizeImageFilling(URL url) {
        BufferedImage image = readImage(url);
        BufferedImage resizedImage = ImageUtil.resizeFilling(image, sFrame.getSize());
        return new ImageIcon(resizedImage);
    }

    private BufferedImage readImage(URL url) {
        try {
            // Get image contents (check to see if there is any)
            byte[] bytes = ImageUtil.read(url);
            if (bytes.length == 0) {
                bytes = ImageUtil.read(BACKGROUND_IMAGE_URL);
            }

            // Create image
            return ImageIO.read(new ByteArrayInputStream(bytes));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Cfg cfg() {
        if (cfg == null) {
            cfg = new Cfg().onChange(this::updateAll);
        }
        return cfg;
    }


    private String logline(Song song, String dance) {
        dance = (dance == null ? "" : dance);
        dance = (dance + "                    ").substring(0, 20);
        String artist = (song.artist().isBlank() ? "" : song.artist() + " - ");
        return "    | " + song.id() + " | " + dance + " | # " + artist + song.name() + " / https://open.spotify.com/track/" + song.id();
    }
}