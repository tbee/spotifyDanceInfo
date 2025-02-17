package org.tbee.spotifyDanceInfo;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.sway.SBorderPanel;
import org.tbee.sway.SContextMenu;
import org.tbee.sway.SDialog;
import org.tbee.sway.SEditorPane;
import org.tbee.sway.SFrame;
import org.tbee.sway.SIconRegistry;
import org.tbee.sway.SLabel;
import org.tbee.sway.SLookAndFeel;
import org.tbee.sway.SOptionPane;
import org.tbee.sway.SStackedPanel;

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
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SpotifyDanceInfo {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyDanceInfo.class);

    public static URL WAITING_IMAGE_URL;
    public static URL BACKGROUND_IMAGE_URL;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private CfgDesktop cfg;
    // Screen
    private SLabel imageSLabel;
    private SEditorPane songSLabel;
    private SEditorPane nextUpSLabel;
    private SEditorPane timeSLabel;
    private SFrame sFrame;

    // Current state
    private Song song = null;
    private List<Song> nextUpSongs = List.of();
    private URL covertArtUrl;

    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (logger.isInfoEnabled()) logger.info("Available fonts: " + Arrays.stream(ge.getAvailableFontFamilyNames()).collect(Collectors.joining(", ")));

        new SpotifyDanceInfo().run();
    }

    private void run() {
        SLookAndFeel.installDefault();
        SIconRegistry.registerDefaultIcons();
        SContextMenu.install();
        CfgDesktop cfg = cfg();

        try {
            WAITING_IMAGE_URL = cfg.waitingImageUrl();
            BACKGROUND_IMAGE_URL = cfg.backgroundImageUrl();

            SwingUtilities.invokeAndWait(() -> {
                imageSLabel = SLabel.of();

                songSLabel = ShadowLabel.of()
                        .onKeyTyped(this::reactToKeyPress)
                        .margin(10, 10, 0, 0)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY);

                nextUpSLabel = ShadowLabel.of()
                        .onKeyTyped(this::reactToKeyPress)
                        .margin(0, 0, 10, 10)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY);

                timeSLabel = ShadowLabel.of()
                        .onKeyTyped(this::reactToKeyPress)
                        .margin(0, 10, 10, 0)
                        .foreground(Color.WHITE)
                        .background(Color.DARK_GRAY);

                SBorderPanel timeBorderPanel = SBorderPanel.of().south(timeSLabel).opaque(false); // for align bottom
                SBorderPanel nextUpBorderPanel = SBorderPanel.of().south(nextUpSLabel).opaque(false); // for align bottom
                SStackedPanel stackPanel = SStackedPanel.of(imageSLabel, timeBorderPanel, nextUpBorderPanel, songSLabel);

                sFrame = SFrame.of(stackPanel)
                        .exitOnClose()
                        .maximize()
                        .undecorated()
                        .title("Spotify Dance Info")
                        .iconImage(readImage(getClass().getResource("/icon.png")))
                        .onKeyTyped(this::reactToKeyPress)
                        .onScreenChange(this::updateAll)
                        .visible(true);

                setFonts();

                // Gather connect information
                ConnectPanel connectPanel = new ConnectPanel(cfg);
                SDialog dialog = SDialog.ofOkCancel(sFrame, "Connect", connectPanel)
                        .onCancel(() -> System.exit(0))
                        .onOk(() -> connect(connectPanel, cfg))
                        .noWindowDecoration()
                        .showAndWait();
            });
        }
        catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // Load initial image
        ImageIcon waitingIcon = readAndResizeImageFilling(WAITING_IMAGE_URL);
        imageSLabel.setIcon(waitingIcon);

        // Start updating the time
        scheduledExecutorService.scheduleAtFixedRate(this::updateTime, 0, 2, TimeUnit.SECONDS);
    }

    private void connect(ConnectPanel connectPanel, CfgDesktop cfg) {
        // process file
        try {
            File file = connectPanel.file();
            cfg.rememberFile(file == null ? "" : file.getAbsolutePath());
            if (file != null) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".tsv")) {
                        cfg.readMoreTracksTSV("web", fileInputStream, 0, 1);
                    }
                    else if (fileName.endsWith(".xlsx")) {
                        cfg.readMoreTracksExcel("web", new XSSFWorkbook(fileInputStream), 0, 0, 1);
                    }
                    else if (fileName.endsWith(".xls")) {
                        cfg.readMoreTracksExcel("web", new HSSFWorkbook(fileInputStream), 0, 0, 1);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        // connect
        new SpotifyWebapi(cfg, connectPanel.clientId(), connectPanel.clientSecret(), connectPanel.redirectUri().toString(), connectPanel.refreshToken())
                .currentlyPlayingCallback(this::updateCurrentlyPlaying)
                .nextUpCallback(this::updateNextUp)
                .coverArtCallback(this::generateAndUpdateImage)
                .connect();
    }

    private void reactToKeyPress(KeyEvent e) {
        if (e.getKeyChar() == 'r') {
            if (logger.isInfoEnabled()) logger.info("Reload");
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

    private void updateTime() {
        SwingUtilities.invokeLater(() -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            timeSLabel.text(time);
        });
    }

    private void updateAll() {
        updateAll(null);
    }
    private void updateAll(Cfg<?> cfg) {
        SwingUtilities.invokeLater(() -> {
            setFonts();
            updateCurrentlyPlaying();
            updateNextUp();
            generateAndUpdateImage();
        });
    }

    private void setFonts() {
        Window window = SFrame.getWindows()[0];
        if (logger.isInfoEnabled()) logger.info("Screen size: " + window.getWidth() + "x" + window.getHeight());

        Font songFont = cfg().songFont(window.getHeight() / 15);
        if (logger.isInfoEnabled()) logger.info("Using song font " + songFont.getFontName() + " " + songFont.getSize());
        songSLabel.font(songFont);

        Font nextFont = cfg().nextFont(window.getHeight() / 20);
        if (logger.isInfoEnabled()) logger.info("Using nextUp font " + nextFont.getFontName() + " " + nextFont.getSize());
        nextUpSLabel.font(nextFont);

        Font timeFont = cfg().timeFont(window.getHeight() / 25);
        if (logger.isInfoEnabled()) logger.info("Using time font " + timeFont.getFontName() + " " + timeFont.getSize());
        timeSLabel.font(timeFont);
    }

    private void generateAndUpdateImage(URL url) {
        SwingUtilities.invokeLater(() -> {
            this.covertArtUrl = url;
            generateAndUpdateImage();
        });
    }

    private void generateAndUpdateImage() {
        if (covertArtUrl == null || !cfg().useCoverArt()) {
            this.imageSLabel.setIcon(readAndResizeImageFilling(song == null ? WAITING_IMAGE_URL : BACKGROUND_IMAGE_URL));
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

        this.imageSLabel.setIcon(new ImageIcon(resizedFillingImage));
    }

    private void updateCurrentlyPlaying(Song song) {
        SwingUtilities.invokeLater(() -> {
            this.song = song;
            updateCurrentlyPlaying();
        });
    }

    private void updateCurrentlyPlaying() {

        try {
            CfgDesktop cfg = cfg();

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
            if (logger.isInfoEnabled()) logger.info(logline);

            // Update screen
            SwingUtilities.invokeLater(() -> {
                songSLabel.text("<html><body><div style=\"text-align:left;\">" + text.toString() + "</div></body></html>");
            });
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(imageSLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateNextUp(List<Song> songs) {
        SwingUtilities.invokeLater(() -> {
            this.nextUpSongs = songs;
            updateNextUp();
        });
    }

    private void updateNextUp() {

        try {
            CfgDesktop cfg = cfg();

            int count = cfg.nextUpCount();
            List<Song> songs = new ArrayList<>(nextUpSongs.subList(0, Math.min(nextUpSongs.size(), count)));

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
                nextUpSLabel.text("<html><body><div style=\"text-align:right;\">" + (text.isEmpty() ? "" : "Next up:") + text.toString() + "</div></body></html>");
            });
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(imageSLabel, e.getMessage(), "Oops", JOptionPane.ERROR_MESSAGE);
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

    private CfgDesktop cfg() {
        if (cfg == null) {
            cfg = new CfgDesktop(true).onChange(this::updateAll);
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