package org.tbee.spotifyDanceInfoWeb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class ControllerBase {

    private static final Logger logger = LoggerFactory.getLogger(ControllerBase.class);

    protected void setVersion(Model model) {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = LocalDateTime.now().toString();
        }
        String versionURL = URLEncoder.encode(version, StandardCharsets.UTF_8);
        model.addAttribute("version", versionURL);
    }

    public static <T> T logException(Throwable t) {
        logger.error(t.getMessage(), t);
        return null;
    }
}
