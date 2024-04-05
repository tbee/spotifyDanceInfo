package org.tbee.spotifyDanceInfo;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.tbee.tecl.TECL;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Cfg {

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
        this("config.tecl", true);
    }

    public Cfg(String configFileName, boolean moreTracksInBackground) {
        this.moreTracksInBackground = moreTracksInBackground;
        try {
            tecl = TECL.parser().findAndParse(configFileName);
            if (tecl == null) {
                System.out.println("No configuration found, switch to default config (local spotify connection)");
                tecl = new TECL("notfound");
            }

            readMoreTracks(tecl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readMoreTracks(TECL tecl) { // can't use the tecl() call here
        // Loop over the moreTrack configurations
        tecl.grps("/moreTracks/tsv").forEach(moreTrackTecl -> {
            if (moreTracksInBackground) {
                executorService.submit(() -> readMoreTracksTSV(moreTrackTecl));
            }
            else {
                readMoreTracksTSV(moreTrackTecl);
            }
        });
    }


    private void readMoreTracksTSV(TECL moreTrack) { // can't use the tecl() call here otherwise there would be an endless loop
        try {
            String uri = moreTrack.str("uri");
            int idIdx = moreTrack.integer("idIdx", 0);
            int danceIdx = moreTrack.integer("danceIdx", 1);

            String contents = readContents(new URI(uri));

            // Parse the contents
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator('\t') // TSV
                    .withIgnoreQuotations(true)
                    .build();
            try (
                CSVReader csvReader = new CSVReaderBuilder(new StringReader(contents))
                        .withSkipLines(1) // skip header
                        .withCSVParser(parser)
                        .withKeepCarriageReturn(false)
                        .build();
            ) {
                System.out.println("contents " + contents);
                csvReader.forEach(line -> {
                    System.out.println("line " + line);

                    // Extract id and dance
                    String id = line[idIdx];
                    String danceText = line[danceIdx];

                    // Possibly split on comma
                    List<String> dances = danceTextToDances(danceText);

                    // Store
                    songIdToDanceNames.put(id, dances);
                });
                System.out.println("Read " + (csvReader.getLinesRead() - 1) + " id(s) from " + uri);
                onChangeListeners.forEach(l -> l.run());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readContents(URI uri) {
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

    private String readContentsHttp(URI uri) {
        try (
                HttpClient httpClient = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();
        ) {
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return httpResponse.body();
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String readContentsFile(URI uri) {
        try {
            File file = new File(uri.toURL().getFile());
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
            }
            Path path = Paths.get(uri.toURL().getFile().substring(1));
            return String.join("\n", Files.readAllLines(path));
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
        return tecl.str(WEBAPI + "/clientId", "");
    }
    public String webapiClientSecret() {
        return tecl.str(WEBAPI + "/clientSecret", "");
    }
    public String webapiRedirect() {
        return tecl.str(WEBAPI + "/redirect", "");
    }
    public String webapiRefreshToken() {
        return tecl.str(WEBAPI + "/refreshToken", "");
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
        return dancesText.contains(",") ? Arrays.asList(dancesText.split(",")) : List.of(dancesText);
    }

    public int nextUpCount() {
        return tecl.integer(SCREEN + "/nextUp/count", 3);
    }

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
