package org.tbee.spotifyDanceInfo.spotifyDanceInfoWeb;

import org.springframework.boot.SpringApplication;
import org.tbee.spotifyDanceInfo.DevRedisConfig;
import org.tbee.spotifyDanceInfoWeb.SpotifyDanceInfoWebApplication;

public class StartApplicationForDev {

    public static void main(String[] args) {
        SpringApplication.from(SpotifyDanceInfoWebApplication::main) // Point to your real main class
                .with(DevRedisConfig.class)
                .run(args);
    }
}
