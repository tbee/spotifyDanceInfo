package org.tbee.spotifySlideshow;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

public interface Spotify {
    void connect();
    Spotify currentlyPlayingCallback(Consumer<Song> currentlyPlayingCallback);
    Spotify nextUpCallback(Consumer<List<Song>> nextUpCallback);
    Spotify coverArtCallback(Consumer<URL> coverArtCallback);
}
