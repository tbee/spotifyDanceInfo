package org.tbee.spotifyDanceInfo;

import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.CC;
import org.tbee.sway.SMigPanel;
import org.tbee.sway.STextField;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ConnectPanel extends SMigPanel {

    private final STextField<String> clientIdTextField = STextField.of(String.class);
    private final STextField<String> clientSecretTextField = STextField.of(String.class);
    private final STextField<URI> redirectUriTextField = STextField.of(URI.class);
    private final STextField<String> refreshTokenTextField = STextField.of(String.class).columns(100);
    private final STextField<String> fileTextField = STextField.of(String.class);

    public ConnectPanel(Cfg cfg) {
        //debug();
        try {
            noGaps();

            clientIdTextField.value(cfg.webapiClientId());
            clientSecretTextField.value(cfg.webapiClientSecret());
            redirectUriTextField.value(new URI(cfg.webapiRedirect().isBlank() ? "https://nyota.softworks.nl/SpotifyDanceInfo.html" : cfg.webapiRedirect()));
            refreshTokenTextField.value(cfg.webapiRefreshToken());

            addHtml("""
                <html><body>
                    <p>
                        Copy the client id and secret from Spotify, as described in the <a href="https://developer.spotify.com/documentation/web-api">getting started</a> section in Spotify's WebAPI documentation. 
                        During the WebAPI set up, use the value below for the redirect URL.
                    </p>
                </body></html>
            """);
            addLabelAndFieldVertical("Redirect URL*", redirectUriTextField);
            addLabelAndFieldVertical("Client id*", clientIdTextField);
            addLabelAndFieldVertical("Client secret*", clientSecretTextField);
            addLabelAndFieldVertical("Refresh Token", refreshTokenTextField);
            //addLabelAndFieldVertical("Track-to-dance file", fileTextField);
            addLabelVertical(" ");
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void addHtml(String html) {
        JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setContentType("text/html");
        jEditorPane.setEditable(false);
        jEditorPane.setBorder(null);
        jEditorPane.setText(html);
        jEditorPane.setPreferredSize(new Dimension(10, 10));
        jEditorPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                    catch (IOException | URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        addFieldVertical(jEditorPane);
    }

    public CC addLabelAndFieldVertical(String label, JComponent component) {
        addLabel(label).alignX(AlignX.LEADING).wrap();
        return addFieldVertical(component);
    }

    public CC addLabelVertical(String label) {
        return addLabel(label).alignX(AlignX.LEADING).growX().gapBottom("10px").wrap();
    }

    public CC addFieldVertical(JComponent component) {
        return addField(component).growX().pushX().gapBottom("10px").wrap();
    }

    public String clientId() {
        return clientIdTextField.getValue();
    }

    public String clientSecret() {
        return clientSecretTextField.getValue();
    }

    public URI redirectUri() {
        return redirectUriTextField.getValue();
    }

    public String refreshToken() {
        return refreshTokenTextField.getValue();
    }
}
