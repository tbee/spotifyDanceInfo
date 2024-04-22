package org.tbee.spotifyDanceInfoQrks;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

public class ThymeleafHelper {
    public static WebContext createContext(HttpServletRequest req, HttpServletResponse res) {
        var application = JakartaServletWebApplication.buildApplication(req.getServletContext());
        var exchange = application.buildExchange(req, res);
        return new WebContext(exchange);
    }
}
