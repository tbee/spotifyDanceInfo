package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.tbee.spotifyDanceInfo.Cfg;

public class CfgSession extends Cfg<CfgSession> {

    public CfgSession(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        super(configFileName, moreTracksInBackground, generateConfigFileIfNotFound);
    }

    @Override
    public void remember(String id, String v) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        request.getSession().setAttribute(id, v);
    }

    @Override
    public String recall(String id) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Object value = request.getSession().getAttribute(id);
        return value == null ? "" : value.toString();
    }
}
