package org.tbee.spotifySlideshow;

import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.Track;
import org.apache.hc.core5.http.ParseException;
import org.tbee.sway.SFrame;
import org.tbee.sway.SLabel;
import org.tbee.sway.SLookAndFeel;
import org.tbee.sway.SStackedPanel;
import org.tbee.sway.support.HAlign;
import org.tbee.sway.support.VAlign;
import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

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
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpotifySlideshow {

    public static final String TRACKS = "/tracks";
    public static final String DANCES = "/dances";
    public final URL undefinedImage;

    // API
    private SpotifyWebapi spotifyWebapi;
    private SpotifyAPI spotifyLocalApi;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    // Screen
    private SLabel sImageLabel;
    private SLabel sTextLabel;
    private SLabel sNextTextLabel;
    private SFrame sFrame;

    // Current state
    private Song song = null;
    private Song nextSong = null;

    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        System.out.print("Available fonts: ");
        Arrays.stream(ge.getAvailableFontFamilyNames()).forEach(f -> System.out.print(f + ", "));
        System.out.println();

        new SpotifySlideshow().run();
    }

    public SpotifySlideshow() {
        undefinedImage = getClass().getResource("/undefined.jpg");
    }

    private void run() {
        SLookAndFeel.installDefault();
        TECL tecl = tecl();

        try {
            SwingUtilities.invokeAndWait(() -> {
                sImageLabel = SLabel.of();

                Font songFont = font(tecl.grp("/screen/songText"), 80);
                System.out.println("Using songFont "+ songFont.getFontName() + " " + songFont.getSize());
                sTextLabel = ShadowLabel.of()
                        .vAlign(VAlign.TOP)
                        .hAlign(HAlign.CENTER)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY)
                        .font(songFont);

                Font nextFont = font(tecl.grp("/screen/nextText"), 40);
                System.out.println("Using nextfont "+ nextFont.getFontName() + " " + nextFont.getSize());
                sNextTextLabel = ShadowLabel.of()
                        .vAlign(VAlign.BOTTOM)
                        .hAlign(HAlign.CENTER)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY)
                        .font(nextFont);

                SStackedPanel stackPanel = SStackedPanel.of(sImageLabel, sNextTextLabel, sTextLabel);

                sFrame = SFrame.of(stackPanel)
                        .exitOnClose()
                        .maximize()
                        .undecorated()
                        .title("Spotify Slideshow")
                        .iconImage(read(getClass().getResource("/icon.png")))
                        .visible(true);
                sFrame.addPropertyChangeListener("graphicsConfiguration", e -> updateScreenSong());
                sFrame.addKeyListener(new KeyListener() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar() == 'r') {
                            updateScreenSong();
                            updateScreenNextUp();
                        }
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {}

                    @Override
                    public void keyReleased(KeyEvent e) {}
                });
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

    private Font font(TECL tecl, int defaultSize) {
        return new Font(tecl.str("font", "Arial"), Font.PLAIN, tecl.integer("fontSize", defaultSize));
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

            // check if the state changed
            boolean playing = (currentlyPlaying != null && currentlyPlaying.getIs_playing());
            Song song = (!playing ? null : new Song(currentlyPlaying.getItem().getId(), "", currentlyPlaying.getItem().getName()));
            boolean songChanged = !Objects.equals(this.song, song);
            if (!songChanged) {
                return;
            }

            // Update screen
            this.song = song;
            updateScreenSong();
            this.nextSong = null;
            updateScreenNextUp();

            // fetch the next song
            if (song != null) {
                scheduledExecutorService.schedule(this::pollSpotifyWebapiAndUpdateNextSong, 0, TimeUnit.SECONDS);
            }
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pollSpotifyWebapiAndUpdateNextSong() {
        try {
            spotifyWebapi.getPlaybackQueue(songs -> {
                this.nextSong = (songs.isEmpty() ? null : songs.get(0));
                updateScreenNextUp();
            });
        }
        catch (RuntimeException | IOException | ParseException | SpotifyWebApiException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pollSpotifyWebapiAndUpdateImage() {
        if (song == null) {
            return;
        }

        try {
            spotifyWebapi.getCoverArt(song.id(), url -> {
                this.sImageLabel.setIcon(readAndResizeImage(url));
            });
        }
        catch (RuntimeException | IOException | ParseException | SpotifyWebApiException e) {
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
                SpotifySlideshow.this.song = new Song(track.getId(), track.getArtist(), track.getName());
                updateScreenSong();
            }

            @Override
            public void onPositionChanged(int position) { }

            @Override
            public void onPlayBackChanged(boolean isPlaying) {
                if (!isPlaying) {
                    SpotifySlideshow.this.song = null;
                    updateScreenSong();
                }
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

    private void updateScreenSong() {

        try {
            TECL tecl = tecl();

            // Determine image and text
            final StringBuilder text = new StringBuilder();
            String image;
            String logline;
            if (this.song == null) {
                image = getClass().getResource("/waiting.jpg").toExternalForm();
                logline = "Nothing is playing";
            }
            else {
                // Get song data
                {
                    String trackId = song.id();
                    List<String> dances = dances(tecl, trackId);
                    String dance = dances.getFirst();
                    image = image(tecl, dance);
                    text.append(song.artist().isBlank() ? "" : song.artist() + "<br>")
                        .append(song.name())
                        .append("<br><hr>")
                        .append(text(tecl, dance));
                    for (String otherDance : dances.subList(1, dances.size())) {
                        String otherText = text(tecl, otherDance);
                        text.append(otherText.isBlank() ? "" : "<br>" + otherText);
                    }
                    logline = logline(song, dance);

                    if (tecl.bool("copySongLoglineToClipboard", false)) {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(logline), null);
                    }
                }
            }
            System.out.println(logline);

            // Load image
            ImageIcon icon = readAndResizeImage(new URI(image).toURL());

            // Update screen
            SwingUtilities.invokeLater(() -> {
                sImageLabel.setIcon(icon);
                sTextLabel.setText("<html><body><div style=\"text-align:center;\">" + text.toString() + "</div></body></html>");
            });
        }
        catch (RuntimeException | URISyntaxException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateScreenNextUp() {

        try {
            TECL tecl = tecl();

            // Determine text
            StringBuilder text = new StringBuilder();
            if (nextSong != null) {
                String trackId = nextSong.id();
                List<String> dances = dances(tecl, trackId);
                String dance = dances.getFirst();
                text.append("Next:")
                        .append("<br>")
                        .append((nextSong.artist() + " " + nextSong.name()).trim())
                        .append("<br>")
                        .append(text(tecl, dance));
                System.out.println(logline(this.nextSong, dance));
            }

            // Update screen
            SwingUtilities.invokeLater(() -> {
                sNextTextLabel.setText("<html><body><div style=\"text-align:center;\">" + text.toString() + "</div></body></html>");
            });
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(sImageLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String text(TECL tecl, String dance) {
        return tecl.grp(DANCES).str("id", dance, "text", "");
    }

    private String image(TECL tecl, String dance) {
        return tecl.grp(DANCES).str("id", dance, "image", undefinedImage.toExternalForm());
    }

    private List<String> dances(TECL tecl, String trackId) {
        String danceConfig = tecl.grp(TRACKS).str("id", trackId, "dance", "undefined");
        return danceConfig.contains(",") ? Arrays.asList(danceConfig.split(",")) : List.of(danceConfig);
    }

    private String logline(Song song, String dance) {
        dance = (dance == null ? "" : dance);
        dance = (dance + "                    ").substring(0, 20);
        String artist = (song.artist().isBlank() ? "" : song.artist() + " - ");
        return "    | " + song.id() + " | " + dance + " | # " + artist + song.name() + " / https://open.spotify.com/track/" + song.id();
    }

    private ImageIcon readAndResizeImage(URL url) {
        BufferedImage image = read(url);
        BufferedImage resizedImage = resize(image, sFrame.getSize());
        return new ImageIcon(resizedImage);
    }

    private BufferedImage read(URL url) {
        try {
            // Get image contents (check to see if there is any)
            byte[] bytes = new byte[]{};
            try (
                InputStream inputStream = url.openStream();
            ) {
                bytes = inputStream.readAllBytes();
            }
            catch (IOException e) {
                System.out.println("Error loading image " + e.getMessage());
            }
            if (bytes.length == 0) {
                try (
                    InputStream inputStream = undefinedImage.openStream();
                ) {
                    bytes = inputStream.readAllBytes();
                }
            }

            // Read image
            return ImageIO.read(new ByteArrayInputStream(bytes));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedImage resize(BufferedImage image, Dimension targetSize) {
        // Read image
        double imageHeight = (double)image.getHeight();
        double imageWidth = (double)image.getWidth();

        // Resize to match frame, but maintain aspect ratio
        double widthScaleFactor = targetSize.getWidth() / imageWidth;
        double heightScaleFactor = targetSize.getHeight() / imageHeight;
        double scaleFactor = Math.max(widthScaleFactor, heightScaleFactor);
        int newWidth = (int)(imageWidth * scaleFactor);
        int newHeight = (int)(imageHeight * scaleFactor);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2.dispose();

        return resizedImage;
    }

    public static TECL tecl() {
        try {
            TECL tecl = TECL.parser().findAndParse();
            return tecl;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}