package org.tbee.spotifySlideshow;

import org.tbee.tecl.TECL;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

import java.io.IOException;

public class SpotifySlideshow {

    public static void main(String[] args) {
        new SpotifySlideshow().run();
    }

    public static TECL tecl() {
        try {
            TECL tecl = TECL.parser().findAndParse("spotifySlideshow.tecl");
            return tecl;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() {

        Spotify spotify = new Spotify(true);
        spotify.connect();

        try {

            for (int i = 0; i < 10; i++) {
                CurrentlyPlaying currentlyPlaying = spotify.getUsersCurrentlyPlayingTrack();
                if (currentlyPlaying != null) {
                    System.out.println("getIs_playing " + currentlyPlaying.getIs_playing());
                    System.out.println("getCurrentlyPlayingType " + currentlyPlaying.getCurrentlyPlayingType());
                    System.out.println("getId " + currentlyPlaying.getItem().getId());
                    System.out.println("getName " + currentlyPlaying.getItem().getName());
                }
                Thread.sleep(5000);
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Problem starting SportifySlideshow", e);
        }
    }
}