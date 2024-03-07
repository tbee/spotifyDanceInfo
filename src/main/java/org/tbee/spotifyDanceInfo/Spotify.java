package org.tbee.spotifyDanceInfo;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

// It is very tempting to maintain the state (currently playing song etc) here.
// And allow external access to that, after all, spotify is playing a song.
// However, since the APIs are asynchronous, there is a real chance that while receiving an event, another one may have update something else.
// So the external listeners should only work with what they get via the callback.

public abstract class Spotify {

    abstract Spotify connect();

    protected Consumer<Song> currentlyPlayingCallback = song -> {};
    protected Consumer<List<Song>> nextUpCallback = songs -> {};
    protected Consumer<URL> coverArtCallback = url -> {};

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
