package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.tbee.spotifyDanceInfo.Cfg;

public class CfgSession extends Cfg<CfgSession> {

    /**
     * This method can only be called within a web server thread.
     */
    static public CfgSession get() {
        return get(SpringUtil.getSession());
    }
    static public CfgSession get(HttpSession session) {
        return (CfgSession)session.getAttribute("cfg");
    }

    public CfgSession(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        super(configFileName, moreTracksInBackground, generateConfigFileIfNotFound);

        SpringUtil.getSession().setAttribute("cfg", this);
    }

    @Override
    public void remember(String id, String v) {
        SpringUtil.getSession().setAttribute(id, v);
    }

    @Override
    public String recall(String id) {
        Object value = SpringUtil.getSession().getAttribute(id);
        return value == null ? "" : value.toString();
    }
}
