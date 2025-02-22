package org.tbee.spotifyDanceInfo;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ParseException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.TECL;
import org.tbee.tutil.RateLimiter;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class Cfg<T> {
    private static final Logger logger = LoggerFactory.getLogger(Cfg.class);

    private static final String CONFIG_TECL = "config.tecl";
    private static final String WEBAPI = "/spotify/webapi";
    private static final String TRACKS = "/tracks";
    private static final String DANCES = "/dances";
    private static final String PLAYLISTS = "/playlists";

    // Total is 25 per 30 seconds, but we want now playing to always be able to run.
    public static final RateLimiter rateLimiter = new RateLimiter("Remaining", 15, Duration.ofSeconds(30));
    public static final RateLimiter rateLimiterCurrentlyPlaying = new RateLimiter("NowPlaying", 10, Duration.ofSeconds(30));

    private static final ExecutorService executorService = Executors.newFixedThreadPool(3); // newCachedThreadPool();
    private final AtomicInteger numberOfBackgroundTasksCounter = new AtomicInteger(0);
    private final List<Throwable> exceptionsInBackgroundTasks = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<Cfg<?>>> onChangeListeners = Collections.synchronizedList(new ArrayList<>());
    private final boolean runInBackground;

    protected TECL tecl;
    protected Map<String, List<String>> songIdToDanceNames = Collections.synchronizedMap(new HashMap<>());
    // Playlist allow for the same track to be in multiple playlists, so the put behavior is different
    protected Map<String, List<String>> songIdToDanceNamesPlaylists = Collections.synchronizedMap(new HashMap<>());

    public Cfg() {
        this(CONFIG_TECL, true, false);
    }

    public Cfg(boolean generateConfigFileIfNotFound) {
        this(CONFIG_TECL, true, generateConfigFileIfNotFound);
    }

    public Cfg(String configFileName, boolean runInBackground, boolean generateConfigFileIfNotFound) {
        this.runInBackground = runInBackground;
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

    public T readMoreTracks() {
        // Loop over the moreTrack configurations
        tecl.grp("/moreTracks/tsv").rows().forEach(moreTrackTecl -> {
            runInBackground(() -> readMoreTracksTSV(moreTrackTecl));
        });
        tecl.grp("/moreTracks/xslx").rows().forEach(moreTrackTecl -> {
            runInBackground(() -> readMoreTracksXSLX(moreTrackTecl));
        });
        tecl.grp("/moreTracks/xsl").rows().forEach(moreTrackTecl -> {
            runInBackground(() -> readMoreTracksXSL(moreTrackTecl));
        });
        return (T)this;
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
            logger.error("Reading TSV failed", e);
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
                if (logger.isDebugEnabled()) logger.debug("Adding track" + id + " at line " + (csvReader.getLinesRead() - 1) + " from " + uri);
                songIdToDanceNames.put(id, dances);
            });
            if (logger.isInfoEnabled()) logger.info("Read " + (csvReader.getLinesRead() - 1) + " track id(s) from " + uri);
            notifyOnChangeListeners();
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
            logger.error("Reading XSLX failed", e);
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
            logger.error("Reading XSL failed", e);
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
            if (logger.isDebugEnabled()) logger.debug("Adding track" + id + " at line " + cnt.get() + " from " + uri);
            songIdToDanceNames.put(id, dances);
        });
        if (logger.isInfoEnabled()) logger.info("Read " + (cnt.get() - 1) + " track id(s) from " + uri);
        notifyOnChangeListeners();
    }

    /**
     * This method must be called after the spotify api was connected.
     */
    public void readPlaylists(Supplier<SpotifyApi> spotifyApiSupplier) {
        tecl.grp(PLAYLISTS).rows().forEach(playlistTecl -> {
            runInBackground(() -> readPlaylist(spotifyApiSupplier, playlistTecl));
        });
    }

    private void readPlaylist(Supplier<SpotifyApi> spotifyApiSupplier, TECL playlistTecl) {
        String danceText = playlistTecl.str("dance");
        List<String> dancesForThisPlaylist = danceTextToDances(danceText);

        try {
            String playlistId = playlistTecl.str("id");
            String playlistName = playlistTecl.str("name", playlistId);
// Not doing this saves one API call which doubles the startup speed
//            rateLimiter.claim("getPlaylist " + playlistId);
//            Playlist playlist = spotifyApiSupplier.get()
//                    .getPlaylist(playlistId).build().execute();

            final int limit = 100;
            int offset = 0;
            int cnt = 0;
            while (offset >= 0) {
                rateLimiter.claim("getPlaylistsItems " + playlistId + "@" + offset);
                Paging<PlaylistTrack> playlistTrackPaging = spotifyApiSupplier.get()
                        .getPlaylistsItems(playlistId)
                        .limit(limit)
                        .offset(offset)
                        .build().execute();
                for (PlaylistTrack playlistTrack : playlistTrackPaging.getItems()) {
                    String trackId = playlistTrack.getTrack().getId();
                    if (logger.isDebugEnabled()) logger.debug("Adding from playlist " + playlistName + ": " + playlistTrack.getTrack().getName() + " as " + dancesForThisPlaylist);

                    // If there are already dances assigned, merge these with the ones for this playlist
                    // This allows for songs to be present in, say, chacha and west coast swing playlists
                    List<String> existingDances = songIdToDanceNamesPlaylists.get(trackId);
                    if (existingDances == null) {
                        existingDances = List.of();
                    }
                    List<String> dances = Stream.concat(existingDances.stream(), dancesForThisPlaylist.stream())
                            .distinct()
                            .toList();

                    songIdToDanceNamesPlaylists.put(trackId, dances);
                    cnt++;
                }
                offset = (playlistTrackPaging.getNext() == null ? -1 : offset + limit);
            }
            if (logger.isInfoEnabled()) logger.info("Read " + cnt + " track id(s) from playlist " + playlistName);
            notifyOnChangeListeners();
        }
        catch (IOException | SpotifyWebApiException | ParseException | RuntimeException e) {
            logger.error("Error reading playlists", e);
            throw new RuntimeException(e);
        }

    }

    // ===========================

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
    protected abstract void remember(String id, String v);
    protected abstract String recall(String id);

    public List<String> trackIdToDanceIds(String trackId) {
        String danceText = tecl.grp(TRACKS).str("id", trackId, "dance", "");
        if (!danceText.isBlank()) {
            return danceTextToDances(danceText);
        }

        // also look in the moreTracks and playlists
        List<String> dances = songIdToDanceNames.get(trackId);
        if (dances == null) {
            dances = songIdToDanceNamesPlaylists.get(trackId);
        }
        return dances == null ? List.of("") : dances;
    }

    public String danceIdToScreenText(String danceId) {
        return tecl.grp(DANCES).str("id", danceId, "text", danceId);
    }

    List<String> danceTextToDances(String dancesText) {
        return (dancesText.contains(",") ? Arrays.asList(dancesText.split(",")) : List.of(dancesText))
                .stream()
                .map(String::trim)
                .toList();
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
    public T onChange(Consumer<Cfg<?>> listener) {
        onChangeListeners.add(listener);
        return (T)this;
    }

    private void runInBackground(Runnable runnable) {
        if (runInBackground) {
            numberOfBackgroundTasksCounter.incrementAndGet();
            notifyOnChangeListeners();
            executorService.submit(() -> {
                try {
                    runnable.run();
                }
                catch (Exception e) {
                    exceptionsInBackgroundTasks.add(e);
                }
                finally {
                    numberOfBackgroundTasksCounter.decrementAndGet();
                    notifyOnChangeListeners();
                }
            });
        }
        else {
            runnable.run();
        }
    }

    public int getNumberOfActiveBackgroundTasks() {
        return numberOfBackgroundTasksCounter.get();
    }

    public int getNumberOfExceptionsInBackgroundTasks() {
        return exceptionsInBackgroundTasks.size();
    }

    private void notifyOnChangeListeners() {
        onChangeListeners.forEach(l -> l.accept(Cfg.this));
    }
}
