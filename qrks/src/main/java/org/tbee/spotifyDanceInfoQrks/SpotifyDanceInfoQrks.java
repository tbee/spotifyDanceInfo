package org.tbee.spotifyDanceInfoQrks;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.tbee.spotifyDanceInfo.Cfg;

@QuarkusMain
public class SpotifyDanceInfoQrks {

    static private final Cfg cfg = new Cfg();

    public static void main(String... args) {
        Quarkus.run(args);
    }

    static Cfg cfg() {
        return cfg;
    }
}
