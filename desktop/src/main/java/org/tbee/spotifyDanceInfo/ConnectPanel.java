package org.tbee.spotifyDanceInfo;

import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.CC;
import org.tbee.sway.SMigPanel;
import org.tbee.sway.STextField;
import org.tbee.sway.format.FileFormat;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.tbee.sway.format.FileFormat.AllowedType.FILE;

public class ConnectPanel extends SMigPanel {

    private final STextField<String> clientIdTextField = STextField.of(String.class);
    private final STextField<String> clientSecretTextField = STextField.of(String.class);
    private final STextField<URI> redirectUriTextField = STextField.of(URI.class);
    private final STextField<String> refreshTokenTextField = STextField.of(String.class).columns(100);
    private final STextField<File> fileTextField = STextField.of(new FileFormat().mustExist(true).allowedType(FILE));

    public ConnectPanel(CfgDesktop cfg) {
        //debug();
        try {
            noGaps();

            clientIdTextField.value(cfg.webapiClientId());
            clientSecretTextField.value(cfg.webapiClientSecret());
            redirectUriTextField.value(new URI(cfg.webapiRedirect().isBlank() ? "https://nyota.softworks.nl/SpotifyDanceInfo.html" : cfg.webapiRedirect()));
            refreshTokenTextField.value(cfg.webapiRefreshToken());
            String filename = cfg.recallReFile();
            if (!filename.isBlank()) {
                fileTextField.value(new File(filename));
            }

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
            addHtml("""
                <html><body>
                    <p>
                        You may provide a <a href="https://spotifydanceinfo.softworks.nl/example.tsv">TSV (tab separated)<a/> or Excel (<a href="https://spotifydanceinfo.softworks.nl/example.xls">xls</a>, <a href="https://spotifydanceinfo.softworks.nl/example.xlsx">xlsx</a>) file that maps track ids to dances. 
                        See the examples for clarification of the points below.
                        <ul>
                            <li>The first column must contain the track id.</li>
                            <li>The second column must contain the dance, or dances separated by a comma.</li>
                            <li>If you use spaces, the dance must be surrounded by double quotes (").</li>
                            <li>The first row/line will not be read, it can contain column headers.</li>
                            <li>Excel files must have the track and dance information on the first sheet.</li>
                        </ul>   
                    </p>
                </body></html>
            """);
            //                          You may also use these abbreviations for the dances.
            addLabelAndFieldVertical("Track-to-dance file", fileTextField);
            addLabelVertical("On Windows: move window to other monitor using Shift+Win+<arrow> keys.");
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
        jEditorPane.setPreferredSize(new Dimension(10, 80));
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

    public File file() {
        return fileTextField.getValue();
    }
}
