package org.tbee.spotifyDanceInfo;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.tbee.sway.SOptionPane;
import org.tbee.tecl.TECL;

import java.awt.Font;
import java.awt.Window;
import java.io.IOException;
import java.io.StringReader;
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

public class Cfg {

    private static final String CONNECT_LOCAL = "local";
    private static final String WEBAPI = "/spotify/webapi";
    private static final String SCREEN = "/screen";
    private static final String BACKGROUNDIMAGE = SCREEN + "/backgroundImage";
    private static final String TRACKS = "/tracks";
    private static final String DANCES = "/dances";

    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private final List<Runnable> onChangeListeners = Collections.synchronizedList(new ArrayList<>());

    private TECL tecl;
    private Map<String, List<String>> songIdToDanceNames = Collections.synchronizedMap(new HashMap<>());


    public Cfg() {
        try {
            tecl = TECL.parser().findAndParse();
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
            executorService.submit(() -> readMoreTracksTSV(moreTrackTecl));
        });
    }


    private void readMoreTracksTSV(TECL moreTrack) { // can't use the tecl() call here otherwise there would be an endless loop
        try {
            String uri = moreTrack.str("uri");
            int idIdx = moreTrack.integer("idIdx", 0);
            int danceIdx = moreTrack.integer("danceIdx", 1);

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
                onChangeListeners.forEach(l -> l.run());
            }
        } catch (URISyntaxException | IOException | InterruptedException | RuntimeException e) {
            e.printStackTrace();
            SOptionPane.ofError(Window.getWindows()[0], "More Tracks", e.getMessage());
        }
    }

    public URL waitingImageUrl() {
        try {
            return tecl.uri(SCREEN + "/waitingImage/uri", SpotifyDanceInfo.class.getResource("/waiting.png").toURI()).toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URL backgroundImageUrl() {
        try {
            return tecl.uri(BACKGROUNDIMAGE + "/uri", SpotifyDanceInfo.class.getResource("/background.png").toURI()).toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    public int backgroundImageNoise() {
        return tecl.integer(BACKGROUNDIMAGE + "/noise", 0);
    }

    public Font songFont() {
        return font(tecl.grp(SCREEN + "/song"), 80);
    }
    public Font nextFont() {
        return font(tecl.grp(SCREEN + "/next"), 40);
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

    List<String> trackIdToDanceIds(String trackId) {
        String danceText = tecl.grp(TRACKS).str("id", trackId, "dance", "");
        if (!danceText.isBlank()) {
            return danceTextToDances(danceText);
        }

        // also look in the moreTracks
        List<String> dances = songIdToDanceNames.get(trackId);
        return dances == null ? List.of("") : dances;
    }

    String danceIdToScreenText(String danceId) {
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
