package org.tbee.spotifyDanceInfoWeb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.tbee.spotifyDanceInfo.Cfg;

@SpringBootApplication
public class SpotifyDanceInfoWebApplication {

	static private Cfg cfg = new Cfg();

	public static void main(String[] args) {
		SpringApplication.run(SpotifyDanceInfoWebApplication.class, args);
	}

	static Cfg cfg() {
		return cfg;
	}
}
