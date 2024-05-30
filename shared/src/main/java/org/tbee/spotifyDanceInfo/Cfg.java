package org.tbee.spotifyDanceInfo;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.TECL;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

public class Cfg {
    private static final Logger logger = LoggerFactory.getLogger(Cfg.class);

    private static final String CONFIG_TECL = "config.tecl";
    private static final String CONNECT_LOCAL = "local";
    private static final String WEBAPI = "/spotify/webapi";
    private static final String SCREEN = "/screen";
    private static final String BACKGROUNDIMAGE = SCREEN + "/backgroundImage";
    private static final String TRACKS = "/tracks";
    private static final String DANCES = "/dances";

    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private final List<Runnable> onChangeListeners = Collections.synchronizedList(new ArrayList<>());
    private final boolean moreTracksInBackground;

    private TECL tecl;
    private Map<String, List<String>> songIdToDanceNames = Collections.synchronizedMap(new HashMap<>());


    public Cfg() {
        this(CONFIG_TECL, true, false);
    }

    public Cfg( boolean generateConfigFileIfNotFound) {
        this(CONFIG_TECL, true, generateConfigFileIfNotFound);
    }

    public Cfg(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        this.moreTracksInBackground = moreTracksInBackground;
        try {
            tecl = TECL.parser().findAndParse(configFileName);
            if (tecl == null) {
                if (generateConfigFileIfNotFound) {
                    createFromExampleConfigFile(configFileName);
                    tecl = TECL.parser().findAndParse(configFileName);
                }
                if (tecl == null) {
                    if (logger.isInfoEnabled()) logger.info("No configuration found, switch to default config for '" + configFileName + "'");
                    tecl = new TECL("notfound");
                }
            }

            readMoreTracks(tecl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createFromExampleConfigFile(String configFileName) throws IOException {
        File file = new File(configFileName);
        if (logger.isInfoEnabled()) logger.info("No configuration found, generating one ìn '" + file.getAbsolutePath() + "'");
        try (
            InputStream inputStream = Cfg.class.getResourceAsStream("/example.config.tecl");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
        ) {
            IOUtils.copy(inputStream, fileOutputStream);
        }
    }

    private void readMoreTracks(TECL tecl) { // can't use the tecl() call here
        // Loop over the moreTrack configurations
        tecl.grps("/moreTracks/tsv").forEach(moreTrackTecl -> {
            runReadMoreTracks(() -> readMoreTracksTSV(moreTrackTecl));
        });
        tecl.grps("/moreTracks/xslx").forEach(moreTrackTecl -> {
            runReadMoreTracks(() -> readMoreTracksXSLX(moreTrackTecl));
        });
        tecl.grps("/moreTracks/xsl").forEach(moreTrackTecl -> {
            runReadMoreTracks(() -> readMoreTracksXSL(moreTrackTecl));
        });
    }

    private void runReadMoreTracks(Runnable runnable) {
        if (moreTracksInBackground) {
            executorService.submit(runnable);
        }
        else {
            runnable.run();
        }
    }

    private void readMoreTracksTSV(TECL moreTrack) { // can't use the tecl() call here otherwise there would be an endless loop
        try {
            String uri = moreTrack.str("uri");
            if (uri == null || uri.isEmpty()) {
                return;
            }
            int idIdx = moreTrack.integer("idIdx", 0);
            int danceIdx = moreTrack.integer("danceIdx", 1);

            InputStream inputStream = readContents(new URI(uri));

            readMoreTracksTSV(uri, inputStream, idIdx, danceIdx);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readMoreTracksTSV(String uri, InputStream inputStream, int idIdx, int danceIdx) throws IOException {
        // Parse the inputStream
        CSVParser parser = new CSVParserBuilder()
                .withSeparator('\t') // TSV
                .withIgnoreQuotations(true)
                .build();
        try (
            CSVReader csvReader = new CSVReaderBuilder(new BufferedReader(new InputStreamReader(inputStream)))
                    .withSkipLines(1) // skip header
                    .withCSVParser(parser)
                    .withKeepCarriageReturn(false)
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
            if (logger.isInfoEnabled()) logger.info("Read " + (csvReader.getLinesRead() - 1) + " id(s) from " + uri);
            onChangeListeners.forEach(l -> l.run());
        }
    }

    private void readMoreTracksXSLX(TECL moreTrack) { // can't use the tecl() call here otherwise there would be an endless loop
        try {
            String uri = moreTrack.str("uri");
            if (uri == null || uri.isEmpty()) {
                return;
            }

            InputStream inputStream = readContents(new URI(uri));

            // Parse the inputStream
            Workbook xssfWorkbook = new XSSFWorkbook(inputStream);
            readMoreTracksExcel(moreTrack, uri, xssfWorkbook);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readMoreTracksXSL(TECL moreTrack) { // can't use the tecl() call here otherwise there would be an endless loop
        try {
            String uri = moreTrack.str("uri");
            if (uri == null || uri.isEmpty()) {
                return;
            }

            InputStream inputStream = readContents(new URI(uri));

            // Parse the inputStream
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook(inputStream);
            readMoreTracksExcel(moreTrack, uri, hssfWorkbook);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void readMoreTracksExcel(TECL moreTrack, String uri, Workbook workbook) {
        int sheetIdx = moreTrack.integer("sheetIdx", 0);
        int idIdx = moreTrack.integer("idIdx", 0);
        int danceIdx = moreTrack.integer("danceIdx", 1);

        readMoreTracksExcel(uri, workbook, sheetIdx, idIdx, danceIdx);
    }

    public void readMoreTracksExcel(String uri, Workbook workbook, int sheetIdx, int idIdx, int danceIdx) {
        Sheet hssfSheet = workbook.getSheetAt(sheetIdx);

        AtomicInteger cnt = new AtomicInteger(0);
        hssfSheet.forEach(row -> {
            // skip first row
            cnt.incrementAndGet();
            if (cnt.get() == 0) {
                return;
            }

            String id = row.getCell(idIdx).getStringCellValue();
            String danceText = row.getCell(danceIdx).getStringCellValue();

            // Possibly split on comma
            List<String> dances = danceTextToDances(danceText);

            // Store
            songIdToDanceNames.put(id, dances);
        });
        if (logger.isInfoEnabled()) logger.info("Read " + (cnt.get() - 1) + " id(s) from " + uri);
        onChangeListeners.forEach(l -> l.run());
    }

    private InputStream readContents(URI uri) {
        if (uri.toString().startsWith("http")) {
            return readContentsHttp(uri);
        }
        if (uri.toString().startsWith("file")) {
            return readContentsFile(uri);
        }
        if (uri.toString().startsWith("./")) {
            try {
                File currentDirectory = new File(".");
                uri = new URI("file:///" + currentDirectory.getAbsolutePath().replace("\\", "/") + "/" + uri);
                return readContentsFile(uri);
            }
            catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }


        throw new IllegalArgumentException("Unknown URI type " + uri);
    }

    private ByteArrayInputStream readContentsHttp(URI uri) {
        try (
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        ) {
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new ByteArrayInputStream(httpResponse.body().getBytes());
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private FileInputStream readContentsFile(URI uri) {
        try {
            File file = new File(uri.toURL().getFile());
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
            }
            return new FileInputStream(file);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public URL waitingImageUrl() {
        try {
            return tecl.uri(SCREEN + "/waitingImage/uri", Cfg.class.getResource("/waiting.png").toURI()).toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URL backgroundImageUrl() {
        try {
            return tecl.uri(BACKGROUNDIMAGE + "/uri", Cfg.class.getResource("/background.png").toURI()).toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    public int backgroundImageNoise() {
        return tecl.integer(BACKGROUNDIMAGE + "/noise", 0);
    }

    public Font songFont(int def) {
        return font(tecl.grp(SCREEN + "/song"), def);
    }
    public Font nextFont(int def) {
        return font(tecl.grp(SCREEN + "/nextUp"), def);
    }
    public Font timeFont(int def) {
        return font(tecl.grp(SCREEN + "/time"), def);
    }
    private Font font(TECL tecl, int defaultSize) {
        return new Font(tecl.str("font", "Arial"), Font.BOLD, tecl.integer("fontSize", defaultSize));
    }

    public String connect() {
        return tecl.str("/spotify/connect", CONNECT_LOCAL);
    }
    public boolean connectLocal() {
        return CONNECT_LOCAL.equalsIgnoreCase(connect());
    }
    public String webapiClientId() {
        return tecl.str(WEBAPI + "/clientId", recallClientId());
    }
    public String webapiClientSecret() {
        return tecl.str(WEBAPI + "/clientSecret", recallClientSecret());
    }
    public String webapiRedirect() {
        return tecl.str(WEBAPI + "/redirect", recallRedirect());
    }
    public String webapiRefreshToken() {
        return tecl.str(WEBAPI + "/refreshToken", recallRefreshToken());
    }

    public void rememberClientId(String v) {
        remember("clientId", v);
    }
    private String recallClientId() {
        return recall("clientId");
    }
    public void rememberClientSecret(String v) {
        remember("clientSecret", v);
    }
    private String recallClientSecret() {
        return recall("clientSecret");
    }
    public void rememberRedirectURI(String v) {
        remember("redirect", v);
    }
    private String recallRedirect() {
        return recall("redirect");
    }
    public void rememberRefreshToken(String v) {
        remember("refreshToken", v);
    }
    private String recallRefreshToken() {
        return recall("refreshToken");
    }
    public void rememberFile(String v) {
        remember("file", v);
    }
    public String recallReFile() {
        return recall("file");
    }
    private void remember(String id, String v) {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        preferences.put(id, v);
    }
    private String recall(String id) {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        return preferences.get(id, "");
    }

    public boolean useCoverArt() {
        return tecl.bool(BACKGROUNDIMAGE + "/useCovertArt", true);
    }

    public List<String> trackIdToDanceIds(String trackId) {
        String danceText = tecl.grp(TRACKS).str("id", trackId, "dance", "");
        if (!danceText.isBlank()) {
            return danceTextToDances(danceText);
        }

        // also look in the moreTracks
        List<String> dances = songIdToDanceNames.get(trackId);
        return dances == null ? List.of("") : dances;
    }

    public String danceIdToScreenText(String danceId) {
        return tecl.grp(DANCES).str("id", danceId, "text", danceId);
    }

    List<String> danceTextToDances(String dancesText) {
        return (dancesText.contains(",") ? Arrays.asList(dancesText.split(",")) : List.of(dancesText))
                .stream()
                .map(s -> s.trim())
                .toList();
    }

    public int nextUpCount() {
        return tecl.integer(SCREEN + "/nextUp/count", 3);
    }

    public List<Abbreviation> getListofDanceAbbreviations() {
        TECL dancesTecl = tecl.grp(DANCES);
        return dancesTecl.strs("id").stream()
                .map(id -> new Abbreviation(id, dancesTecl.str("id", id, "text", "?")))
                .toList();
    }
    public record Abbreviation(String id, String name) {}

    public boolean copyTrackLoglineToClipboard() {
        return tecl.bool("copyTrackLoglineToClipboard", false);
    }

    /**
     * Some configurations are read in the background, and after one completes the onChange listeners are called.
     * @param listener
     * @return
     */
    public Cfg onChange(Runnable listener) {
        onChangeListeners.add(listener);
        return this;
    }
}
