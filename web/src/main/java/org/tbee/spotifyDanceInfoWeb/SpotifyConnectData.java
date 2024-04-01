package org.tbee.spotifyDanceInfoWeb;

public class SpotifyConnectData {
    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private String refreshToken;
    private String accessToken;

    public String clientId() {
        return clientId;
    }

    public SpotifyConnectData clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public SpotifyConnectData clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public String redirectUrl() {
        return redirectUrl;
    }

    public SpotifyConnectData redirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
        return this;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public SpotifyConnectData refreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public String accessToken() {
        return accessToken;
    }

    public SpotifyConnectData accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }
}
