package org.tbee.spotifyDanceInfoWeb;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {
    private Song currentlyPlaying = new Song();
    private List<Song> nextUp = new ArrayList<>();

    public Song currentlyPlaying() {
        return currentlyPlaying;
    }

    public ScreenData currentlyPlaying(Song v) {
        this.currentlyPlaying = v;
        return this;
    }

    public List<Song> nextUp() {
        return nextUp;
    }
    public ScreenData nextUp(List<Song> v) {
        nextUp = v;
        return this;
    }
}
