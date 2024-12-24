package org.tbee.spotifyDanceInfoWeb;

import org.tbee.spotifyDanceInfo.Cfg;

public class CfgSession extends Cfg<CfgSession> {

    static public CfgSession get() {
        return (CfgSession)SpringUtil.getRequest().getSession().getAttribute("cfg");
    }

    public CfgSession(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        super(configFileName, moreTracksInBackground, generateConfigFileIfNotFound);

        SpringUtil.getRequest().getSession().setAttribute("cfg", this);
    }

    @Override
    public void remember(String id, String v) {
        SpringUtil.getRequest().getSession().setAttribute(id, v);
    }

    @Override
    public String recall(String id) {
        Object value = SpringUtil.getRequest().getSession().getAttribute(id);
        return value == null ? "" : value.toString();
    }
}
