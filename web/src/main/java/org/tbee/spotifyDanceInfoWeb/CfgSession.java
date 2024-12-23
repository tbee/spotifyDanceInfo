package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.tbee.spotifyDanceInfo.Cfg;

import java.util.Objects;

public class CfgSession extends Cfg<CfgSession> {

    static public CfgSession get() {
        return (CfgSession)getRequest().getSession().getAttribute("cfg");
    }

    public CfgSession(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        super(configFileName, moreTracksInBackground, generateConfigFileIfNotFound);

        getRequest().getSession().setAttribute("cfg", this);
    }

    @Override
    public void remember(String id, String v) {
        getRequest().getSession().setAttribute(id, v);
    }

    @Override
    public String recall(String id) {
        Object value = getRequest().getSession().getAttribute(id);
        return value == null ? "" : value.toString();
    }

    private static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }
}
