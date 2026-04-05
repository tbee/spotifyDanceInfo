package org.tbee.spotifyDanceInfoWeb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// REDIS:
// The way the application works, with background threads updating data like the song-to-dance map and the ScreenData class,
// but more importantly the RateLimiter that synchronizes Spotify's API access across multiple threads,
// makes converting this application to high available difficult.
// TECL is not intended for serialization, so you need to convert it into some kind of serializable Map<Song,List<Dance>>.
// And the RateLimiter can be serialized, but you need some kind of cross container atomicity when claiming a token.
// Redis does support this, but it will require a refactor of the application to use Redis as a database.
// You cannot simply serialize the session.

@SpringBootApplication
public class SpotifyDanceInfoWebApplication {

	static private CfgApp cfg = new CfgApp().read();

	public static void main(String[] args) {
		SpringApplication.run(SpotifyDanceInfoWebApplication.class, args);
	}

	static CfgApp cfg() {
		return cfg;
	}
}
