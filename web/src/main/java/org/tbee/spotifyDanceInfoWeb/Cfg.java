package org.tbee.spotifyDanceInfoWeb;

import org.tbee.tecl.TECL;

import java.io.IOException;

public class Cfg {

    private static final String WEBAPI = "/spotify/webapi";
    private TECL tecl;

    public Cfg() {
        try {
            tecl = TECL.parser().findAndParse();
            if (tecl == null) {
                tecl = new TECL("notfound");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String webapiClientId() {
        return tecl.str(WEBAPI + "/clientId", "");
    }
    public String webapiClientSecret() {
        return tecl.str(WEBAPI + "/clientSecret", "");
    }
    public String webapiRedirect() {
        return tecl.str(WEBAPI + "/redirect", "");
    }
    public String webapiRefreshToken() {
        return tecl.str(WEBAPI + "/refreshToken", "");
    }
}
