package org.tbee.spotifyDanceInfo;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.tbee.sway.SFrame;
import org.tbee.sway.SLabel;
import org.tbee.sway.SLookAndFeel;
import org.tbee.sway.SOptionPane;
import org.tbee.sway.SStackedPanel;
import org.tbee.sway.support.HAlign;
import org.tbee.sway.support.VAlign;
import org.tbee.tecl.TECL;

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
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpotifyDanceInfo {

    public static final String TRACKS = "/tracks";
    public static final String DANCES = "/dances";

    public static final URL WAITING_IMAGE_URL;
    public static final URL UNDEFINED_IMAGE_URL;

    static {
        WAITING_IMAGE_URL = SpotifyDanceInfo.class.getResource("/waiting.png");
        UNDEFINED_IMAGE_URL = SpotifyDanceInfo.class.getResource("/undefined.png");
    }

    private TECL tecl;
    private Map<String, List<String>> songIdToDanceNames = Map.of();

    // Screen
    private SLabel sImageLabel;
    private SLabel sTextLabel;
    private SLabel sNextTextLabel;
    private SFrame sFrame;

    // Current state
    private Song song = null;
    private List<Song> nextUpSongs = List.of();

    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        System.out.print("Available fonts: ");
        Arrays.stream(ge.getAvailableFontFamilyNames()).forEach(f -> System.out.print(f + ", "));
        System.out.println();

        new SpotifyDanceInfo().run();
    }

    private void run() {
        SLookAndFeel.installDefault();
        TECL tecl = tecl();

        try {
            SwingUtilities.invokeAndWait(() -> {
                sImageLabel = SLabel.of();

                Font songTecl = font(tecl.grp("/screen/song"), 80);
                System.out.println("Using songFont "+ songTecl.getFontName() + " " + songTecl.getSize());
                sTextLabel = ShadowLabel.of()
                        .vAlign(VAlign.TOP)
                        .hAlign(HAlign.LEFT)
                        .margin(10, 10, 0, 0)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY)
                        .font(songTecl);

                Font nextFont = font(tecl.grp("/screen/next"), 40);
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
        String CONNECT_LOCAL = "local";
        String connect = tecl().str("/spotify/connect", CONNECT_LOCAL);
        (CONNECT_LOCAL.equalsIgnoreCase(connect) ? new SpotifyLocalApi() : new SpotifyWebapi(tecl))
                .currentlyPlayingCallback(this::updateCurrentlyPlaying)
                .nextUpCallback(this::updateNextUp)
                .coverArtCallback(this::generateAndUpdateImage)
                .connect();
    }

    private void reactToKeyPress(KeyEvent e) {
        if (e.getKeyChar() == 'r') {
            tecl = null; // force reload
            updateCurrentlyPlaying();
            updateNextUp();
        }
        else if (e.getKeyChar() == 'q') {
            System.exit(0);
        }
//        else {
//            SOptionPane.ofInfo(sFrame, "Supported keys",
//                        """
//                        (q)uit
//                        (r)eload configuration
//                        """);
//        }
    }

    private void generateAndUpdateImage(URL url) {
        boolean useCoverArt = tecl().bool("/screen/useCovertArt", true);
        if (url == null || !useCoverArt) {
            this.sImageLabel.setIcon(readAndResizeImageFilling(UNDEFINED_IMAGE_URL));
            return;
        }

        BufferedImage image = readImage(url);

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

        //resizedFillingImage = ImageUtil.addGaussianBlur(resizedFillingImage, 2.0);

        this.sImageLabel.setIcon(new ImageIcon(resizedFillingImage));
    }

    private void updateCurrentlyPlaying(Song song) {
        this.song = song;
        updateCurrentlyPlaying();
    }

    private void updateCurrentlyPlaying() {

        try {
            TECL tecl = tecl();

            // Determine image and text
            final StringBuilder text = new StringBuilder();
            String logline;
            if (this.song == null) {
                logline = "Nothing is playing";
            }
            else {
                String trackId = song.id();
                List<String> danceIds = trackIdToDanceIds(tecl, trackId);
                text.append(song.artist().isBlank() ? "" : song.artist() + "<br>")
                    .append(song.name())
                    .append("<br>");
                for (String danceId : danceIds) {
                    String screenText = danceIdToScreenText(tecl, danceId);
                    text.append(screenText.isBlank() ? "" : "<br>" + screenText);
                }
                logline = logline(song, danceIds.isEmpty() ? "" : danceIds.getFirst());

                if (tecl.bool("copyTrackLoglineToClipboard", false)) {
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
            TECL tecl = tecl();

            int count = tecl.integer("/screen/nextUp/count", 3);
            List<Song> songs = nextUpSongs.subList(0, Math.min(nextUpSongs.size(), count));

            // Determine text
            StringBuilder text = new StringBuilder();
            songs.forEach(nextSong -> {
                String trackId = nextSong.id();
                text.append("<br><br>")
                    .append(nextSong.artist().isBlank() ? "" : nextSong.artist() + " - ")
                    .append(nextSong.name());
                trackIdToDanceIds(tecl, trackId).stream()
                    .filter(dance -> dance != null && !dance.isBlank())
                    .forEach(danceId -> {
                        text.append("<br>")
                            .append(danceIdToScreenText(tecl, danceId));
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
                bytes = ImageUtil.read(UNDEFINED_IMAGE_URL);
            }

            // Create image
            return ImageIO.read(new ByteArrayInputStream(bytes));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TECL tecl() {
        if (tecl != null) {
            return tecl;
        }

        try {
            tecl = TECL.parser().findAndParse();
            if (tecl == null) {
                tecl = new TECL("notfound");
                tecl.populateConvertFunctions(); // TBEERNOT can we remove this?
            }
            readMoreTracks(tecl);
            return tecl;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readMoreTracks(TECL tecl) { // can't use the tecl() call here
        Map<String, List<String>> songIdToDanceNames = new HashMap<>();

        // Loop over the moreTrack configurations
        tecl.grps("/moreTracks").forEach(moreTrackTecl -> {
            String type = moreTrackTecl.str("type", "");
            if ("TSV".equalsIgnoreCase(type)) {
                readMoreTracksTSV(moreTrackTecl, songIdToDanceNames);
            }
            else {
                SOptionPane.ofError(sFrame, "More Tracks", "Don't understand moreTracks type '" + type + "'");
            }
        });

        // Replace with new data
        this.songIdToDanceNames = songIdToDanceNames;
    }

    private void readMoreTracksTSV(TECL moreTrack, Map<String, List<String>> songIdToDanceNames) { // can't use the tecl() call here otherwise there would be an endless loop
        String uri = moreTrack.str("uri");
        int idIdx = moreTrack.integer("idIdx", 0);
        int danceIdx = moreTrack.integer("danceIdx", 1);
        try {
            // First read the URI contents
            String contents = "";
            try (
                HttpClient httpClient = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();
            ) {
                HttpRequest request = HttpRequest.newBuilder(new URI(uri)).GET().build();
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                contents = httpResponse.body();
            }

            // Parse the contents
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator('\t') // TSV
                    .withIgnoreQuotations(true)
                    .build();
            try (
                CSVReader csvReader = new CSVReaderBuilder(new StringReader(contents))
                        .withSkipLines(1) // skip header
                        .withCSVParser(parser)
                        .build();
            ) {
                csvReader.forEach(line -> {

                    // Extract id and dance
                    String id = line[idIdx];
                    String danceText = line[danceIdx];

                    // Possibly split on comma
                    List<String> dances = danceTextToDances(danceText);

                    // Store
                    songIdToDanceNames.put(id, dances);
                });
                System.out.println("Read " + (csvReader.getLinesRead() - 1) + " id(s) from " + uri);
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            SOptionPane.ofError(sFrame, "More Tracks", e.getMessage());
            return;
        }
    }

    private static List<String> danceTextToDances(String dancesText) {
        return dancesText.contains(",") ? Arrays.asList(dancesText.split(",")) : List.of(dancesText);
    }

    private Font font(TECL tecl, int defaultSize) {
        return new Font(tecl.str("font", "Arial"), Font.BOLD, tecl.integer("fontSize", defaultSize));
    }

    private static String danceIdToScreenText(TECL tecl, String danceId) {
        return tecl.grp(DANCES).str("id", danceId, "text", danceId);
    }

    private List<String> trackIdToDanceIds(TECL tecl, String trackId) {
        String danceText = tecl.grp(TRACKS).str("id", trackId, "dance", "");
        if (!danceText.isBlank()) {
            return danceTextToDances(danceText);
        }

        // also look in the moreTracks
        List<String> dances = songIdToDanceNames.get(trackId);
        return dances == null ? List.of("") : dances;
    }

    private String logline(Song song, String dance) {
        dance = (dance == null ? "" : dance);
        dance = (dance + "                    ").substring(0, 20);
        String artist = (song.artist().isBlank() ? "" : song.artist() + " - ");
        return "    | " + song.id() + " | " + dance + " | # " + artist + song.name() + " / https://open.spotify.com/track/" + song.id();
    }
}