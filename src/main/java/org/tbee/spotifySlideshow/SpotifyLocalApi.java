package org.tbee.spotifySlideshow;

import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.Track;

import java.net.URL;

public class SpotifyLocalApi extends Spotify {

    private SpotifyAPI spotifyLocalApi;

    protected Song lastPlayingSong = null;

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
                lastPlayingSong = new Song(track.getId(), track.getArtist(), track.getName());
                currentlyPlayingCallback.accept(lastPlayingSong);

                coverArtCallback.accept(undefinedImageUrl);
            }

            @Override
            public void onPositionChanged(int position) { }

            @Override
            public void onPlayBackChanged(boolean isPlaying) {
                currentlyPlayingCallback.accept(!isPlaying ? null : lastPlayingSong);
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
