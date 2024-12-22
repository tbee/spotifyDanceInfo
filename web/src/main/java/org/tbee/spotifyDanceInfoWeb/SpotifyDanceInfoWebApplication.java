package org.tbee.spotifyDanceInfoWeb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpotifyDanceInfoWebApplication {

	static private CfgApp cfg = new CfgApp();

	public static void main(String[] args) {
		SpringApplication.run(SpotifyDanceInfoWebApplication.class, args);
	}

	static CfgApp cfg() {
		return cfg;
	}
}
