package org.tbee.spotifySlideshow;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

public abstract class Spotify {

    abstract Spotify  connect();

    protected Consumer<Song> currentlyPlayingCallback = song -> {};
    protected Consumer<List<Song>> nextUpCallback = songs -> {};
    protected Consumer<URL> coverArtCallback = url -> {};

    protected Song currentPlayingSong = null;

    public Spotify currentlyPlayingCallback(Consumer<Song> currentlyPlayingCallback) {
        this.currentlyPlayingCallback = currentlyPlayingCallback;
        return this;
    }

    public Spotify nextUpCallback(Consumer<List<Song>> nextUpCallback) {
        this.nextUpCallback = nextUpCallback;
        return this;
    }

    public Spotify coverArtCallback(Consumer<URL> coverArtCallback) {
        this.coverArtCallback = coverArtCallback;
        return this;
    }
}
