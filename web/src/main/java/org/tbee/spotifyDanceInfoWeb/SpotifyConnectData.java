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

    public SpotifyConnectData clientId(String v) {
        this.clientId = v;
        return this;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public SpotifyConnectData clientSecret(String v) {
        this.clientSecret = v;
        return this;
    }

    public String redirectUrl() {
        return redirectUrl;
    }

    public SpotifyConnectData redirectUrl(String v) {
        this.redirectUrl = v;
        return this;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public SpotifyConnectData refreshToken(String v) {
        this.refreshToken = v;
        return this;
    }

    public String accessToken() {
        return accessToken;
    }

    public SpotifyConnectData accessToken(String v) {
        this.accessToken = v;
        return this;
    }
}
