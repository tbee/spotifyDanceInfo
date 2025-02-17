package org.tbee.spotifyDanceInfoWeb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerBase {

    private static final Logger logger = LoggerFactory.getLogger(ControllerBase.class);


    public static <T> T logException(Throwable t) {
        logger.error(t.getMessage(), t);

        // just in case something went wrong with scheduled refreshing
        if (t.getMessage().contains("The access token expired")) {
            SpotifyConnectData.get(SpringUtil.getSession()).refreshAccessToken();
        }

        return null;
    }
}
