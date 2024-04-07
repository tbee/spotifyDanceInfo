package org.tbee.spotifyDanceInfoWeb;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> cookieProcessorCustomizer() {
        return (TomcatServletWebServerFactory factory) -> {

            // also listen on http?
            if (LOG.isInfoEnabled()) LOG.info("server.http.port = " + httpPort);
            if (httpPort != 0) {
                if (LOG.isInfoEnabled()) LOG.info("Also listen on http " + httpPort);
                final Connector connector = new Connector();
                connector.setPort(httpPort);
                factory.addAdditionalTomcatConnectors(connector);
            }
        };
    }

    @Value("${server.http.port}")
    private int httpPort;
}
