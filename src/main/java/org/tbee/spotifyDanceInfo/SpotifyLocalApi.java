package org.tbee.spotifyDanceInfo;

import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.Track;

public class SpotifyLocalApi extends Spotify {

    private SpotifyAPI spotifyLocalApi;

    protected Song lastPlayingSong = null;

    public Spotify connect() {

        spotifyLocalApi = SpotifyAPIFactory.create();
        spotifyLocalApi.registerListener(new SpotifyListener() {
            @Override
            public void onConnect() {
            }

            @Override
            public void onTrackChanged(Track track) {
                lastPlayingSong = new Song(track.getId(), track.getArtist(), track.getName());
                currentlyPlayingCallback.accept(lastPlayingSong);

                coverArtCallback.accept(SpotifyDanceInfo.UNDEFINED_IMAGE_URL);
            }

            @Override
            public void onPositionChanged(int position) { }

            @Override
            public void onPlayBackChanged(boolean isPlaying) {
                currentlyPlayingCallback.accept(!isPlaying ? null : lastPlayingSong);
                coverArtCallback.accept(!isPlaying ? SpotifyDanceInfo.WAITING_IMAGE_URL : SpotifyDanceInfo.UNDEFINED_IMAGE_URL);
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
