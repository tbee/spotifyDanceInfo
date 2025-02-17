package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.tbee.spotifyDanceInfo.Cfg;

public class CfgSession extends Cfg<CfgSession> {

    static public CfgSession get(HttpSession session) {
        return (CfgSession)session.getAttribute(CfgSession.class.getName());
    }

    public CfgSession(HttpSession session) {
        super();
        session.setAttribute(CfgSession.class.getName(), this);
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
