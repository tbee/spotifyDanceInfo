package org.tbee.spotifyDanceInfoWeb;

public class SpotifyScreenData {
    private String currentlyPlayingTrackId;
    private String currentlyPlayingSongTitle;
    private String currentlyPlayingSongArtist;

    public String currentlyPlayingTrackId() {
        return currentlyPlayingTrackId;
    }

    public SpotifyScreenData currentlyPlayingTrackId(String clientId) {
        this.currentlyPlayingTrackId = clientId;
        return this;
    }

    public String currentlyPlayingSongTitle() {
        return currentlyPlayingSongTitle;
    }

    public SpotifyScreenData currentlyPlayingSongTitle(String clientSecret) {
        this.currentlyPlayingSongTitle = clientSecret;
        return this;
    }

    public String currentlyPlayingSongArtist() {
        return currentlyPlayingSongArtist;
    }

    public SpotifyScreenData currentlyPlayingSongArtist(String redirectUrl) {
        this.currentlyPlayingSongArtist = redirectUrl;
        return this;
    }
}
