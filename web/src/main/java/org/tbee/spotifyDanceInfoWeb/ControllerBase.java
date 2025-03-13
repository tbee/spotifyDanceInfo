package org.tbee.spotifyDanceInfoWeb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import java.time.LocalDateTime;

public class ControllerBase {

    private static final Logger logger = LoggerFactory.getLogger(ControllerBase.class);

    protected void setVersion(Model model) {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = LocalDateTime.now().toString();
        }
        System.out.println("version: " + version);
        model.addAttribute("version", version);
    }

    public static <T> T logException(Throwable t) {
        logger.error(t.getMessage(), t);

        // just in case something went wrong with scheduled refreshing
        if (t.getMessage().contains("The access token expired")) {
            SpotifyConnectData.get(SpringUtil.getSession()).refreshAccessToken();
        }

        return null;
    }
}
