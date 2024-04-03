package org.tbee.spotifyDanceInfoWeb;

import java.util.ArrayList;
import java.util.List;

public class Song {
    private String trackId = "";
    private String title = "";
    private String artist = "";
    private List<String> dances = new ArrayList<>();

    public Song() {

    }
    public Song(String trackId, String title, String artist) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
    }
    public String trackId() {
        return trackId;
    }

    public Song trackId(String v) {
        this.trackId = v;
        return this;
    }

    public String title() {
        return title;
    }

    public Song title(String v) {
        this.title = v;
        return this;
    }

    public String artist() {
        return artist;
    }

    public Song artist(String v) {
        this.artist = v;
        return this;
    }

    public List<String> dances() {
        return dances;
    }

    public Song dances(List<String> v) {
        this.dances = v;
        return this;
    }
}
