package org.tbee.spotifyDanceInfoQrks;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class SpotifyDanceInfoQrks {

    static private final CfgWeb cfg = new CfgWeb();

    public static void main(String... args) {
        Quarkus.run(args);
    }

    static CfgWeb cfg() {
        return cfg;
    }
}
