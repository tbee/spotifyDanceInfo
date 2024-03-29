package org.tbee.spotifyDanceInfo;

public record Song(String id, String artist, String name) {

    Song withArtist(String artist) {
        return new Song(this.id, artist, this.name);
    }
}
