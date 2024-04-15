package org.tbee.spotifyDanceInfoWeb;

import java.time.LocalDateTime;

public class SpotifyConnectData {
    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private String refreshToken;
    private String accessToken;
    private LocalDateTime accessTokenExpireDateTime;
    private LocalDateTime connectTime = null;

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

    public LocalDateTime accessTokenExpireDateTime() {
        return accessTokenExpireDateTime;
    }

    public SpotifyConnectData accessTokenExpireDateTime(LocalDateTime v) {
        this.accessTokenExpireDateTime = v;
        return this;
    }


    public LocalDateTime connectTime() {
        return connectTime;
    }
    public SpotifyConnectData connectTime(LocalDateTime v) {
        this.connectTime = v;
        return this;
    }
}
