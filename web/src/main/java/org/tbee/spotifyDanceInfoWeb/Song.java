package org.tbee.spotifyDanceInfoWeb;

import java.util.ArrayList;
import java.util.List;

public class Song {
    private String trackId = "trackid";
    private String title = "title";
    private String artist = "artist";
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

    public Song trackId(String clientId) {
        this.trackId = clientId;
        return this;
    }

    public String title() {
        return title;
    }

    public Song title(String clientSecret) {
        this.title = clientSecret;
        return this;
    }

    public String artist() {
        return artist;
    }

    public Song artist(String redirectUrl) {
        this.artist = redirectUrl;
        return this;
    }
}
