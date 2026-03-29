package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.Session;
import org.tbee.spotifyDanceInfo.Cfg;

import java.io.Serializable;

public class CfgSession extends Cfg<CfgSession> implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CfgSession.class);

    static public CfgSession get(HttpSession session) {
        CfgSession cfgSession = (CfgSession) session.getAttribute(CfgSession.class.getName());
        if (LOGGER.isDebugEnabled()) LOGGER.debug("CfgSession retrieved from HTTP session " + session.getId() + " -> " + cfgSession);
        return cfgSession;
    }

    static public CfgSession get(Session session) {
        CfgSession cfgSession = session.getAttribute(CfgSession.class.getName());
        if (LOGGER.isDebugEnabled()) LOGGER.debug("CfgSession retrieved from Spring session " + session.getId() + " -> " + cfgSession);
        return cfgSession;
    }

    public CfgSession storeIn(HttpSession session) {
        session.setAttribute(CfgSession.class.getName(), this);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("CfgSession stored in HTTP session " + session.getId() + " -> " + this);
        return this;
    }
    public CfgSession storeIn(Session session) {
        session.setAttribute(CfgSession.class.getName(), this);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("CfgSession stored in Spring session " + session.getId() + " -> " + this);
        return this;
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
