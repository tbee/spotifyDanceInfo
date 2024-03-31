package org.tbee.spotifyDanceInfo;

import org.apache.hc.core5.http.ParseException;
import org.tbee.sway.SDialog;
import org.tbee.sway.SLabel;
import org.tbee.sway.SOptionPane;
import org.tbee.sway.SVPanel;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;

public class SpotifyWebapiSwing extends SpotifyWebapi {

    private final Cfg cfg;
    private String refreshToken = null;

    public SpotifyWebapiSwing(Cfg cfg) {
        super(cfg);
        this.cfg = cfg;
    }

    public Spotify connect() {
        try {
            // Do we have tokens stored or need to fetch them?
            String refreshToken = cfg.webapiRefreshToken();
            if (refreshToken.isBlank()) {

                // Open spotify in the browser
                // https://developer.spotify.com/documentation/web-api/concepts/authorization
                // The authorizationCodeUri must be opened in the browser, the resulting code (in the redirect URL) pasted into the popup
                // The code can only be used once to connect
                URI authorizationCodeUri = spotifyApi.authorizationCodeUri()
                        .scope("user-read-playback-state,user-read-currently-playing")
                        .build().execute();
                System.out.println("authorizationCodeUri " + authorizationCodeUri);
                Desktop.getDesktop().browse(authorizationCodeUri);

                // Ask for the authorization code
                Window window = Window.getWindows()[0];
                var authorizationCode = SOptionPane.showInputDialog(window, "Please copy the authorization code here");
                if (authorizationCode == null || authorizationCode.isBlank()) {
                    String message = "Authorization code cannot be empty";
                    javax.swing.JOptionPane.showMessageDialog(window, message);
                    throw new IllegalArgumentException(message);
                }

                // Login to spotify and get the refresh and access tokens
                AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCode(authorizationCode).build().execute();
                refreshToken = authorizationCodeCredentials.getRefreshToken();
                System.out.println("refreshToken " + refreshToken);

                // Suggest to copy the refresh token in the configuration file
                String refreshTokenCopy = "\"" + refreshToken + "\"";
                if (SDialog.ofOkCancel(window, "",
                        SVPanel.of(
                                SLabel.of("Do you want to copy the text below?"),
                                SLabel.of(refreshTokenCopy).font(SLabel.of().getFont().deriveFont(Font.BOLD)),
                                SLabel.of("It can be placed as the refreshToken in the configuration file for easy startup.")
                        )
                ).showAndWait().closeReasonIsOk()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(refreshTokenCopy), null);
                }
            }

            // connect
            super.connect(refreshToken);
            return this;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new RuntimeException("Problem connecting to Sportify webapi", e);
        }
    }

}
