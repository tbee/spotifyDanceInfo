package org.tbee.spotifySlideshow;

import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.Track;

import java.net.URL;

public class SpotifyLocalApi extends Spotify {

    private SpotifyAPI spotifyLocalApi;

    public Spotify connect() {
        URL waitingImageUrl = getClass().getResource("/waiting.jpg");
        URL undefinedImageUrl = getClass().getResource("/undefined.jpg");

        spotifyLocalApi = SpotifyAPIFactory.create();
        spotifyLocalApi.registerListener(new SpotifyListener() {
            @Override
            public void onConnect() {
            }

            @Override
            public void onTrackChanged(Track track) {
                currentPlayingSong = new Song(track.getId(), track.getArtist(), track.getName());
                currentlyPlayingCallback.accept(currentPlayingSong);

                coverArtCallback.accept(undefinedImageUrl);
            }

            @Override
            public void onPositionChanged(int position) { }

            @Override
            public void onPlayBackChanged(boolean isPlaying) {
                currentlyPlayingCallback.accept(!isPlaying ? null : currentPlayingSong);
                coverArtCallback.accept(!isPlaying ? waitingImageUrl : undefinedImageUrl);
            }

            @Override
            public void onSync() { }

            @Override
            public void onDisconnect(Exception exception) {
                currentlyPlayingCallback.accept(null);
                exception.printStackTrace();
                spotifyLocalApi.stop();
            }
        });
        spotifyLocalApi.initialize();

        return this;
    }
}
