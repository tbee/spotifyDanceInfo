package org.tbee.spotifyDanceInfo;

import org.tbee.tecl.TECL;

import java.awt.Font;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.prefs.Preferences;

public class CfgDesktop extends org.tbee.spotifyDanceInfo.Cfg<CfgDesktop> {
    protected static final String SCREEN = "/screen";
    protected static final String BACKGROUNDIMAGE = SCREEN + "/backgroundImage";


    public CfgDesktop() {
        super();
        readMoreTracks();
    }

    public CfgDesktop(boolean generateConfigFileIfNotFound) {
        super(generateConfigFileIfNotFound);
        readMoreTracks();
    }

    @Override
    public void remember(String id, String v) {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        preferences.put(id, v);
    }

    @Override
    public String recall(String id) {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        return preferences.get(id, "");
    }

    public Font songFont(int def) {
        return font(tecl.grp(SCREEN + "/song"), def);
    }
    public Font nextFont(int def) {
        return font(tecl.grp(SCREEN + "/nextUp"), def);
    }
    public Font timeFont(int def) {
        return font(tecl.grp(SCREEN + "/time"), def);
    }
    private Font font(TECL tecl, int defaultSize) {
        return new Font(tecl.str("font", "Arial"), Font.BOLD, tecl.integer("fontSize", defaultSize));
    }

    public URL waitingImageUrl() {
        try {
            return tecl.uri(SCREEN + "/waitingImage/uri", Cfg.class.getResource("/waiting.png").toURI()).toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean useCoverArt() {
        return tecl.bool(BACKGROUNDIMAGE + "/useCovertArt", true);
    }

    public URL backgroundImageUrl() {
        try {
            return tecl.uri(BACKGROUNDIMAGE + "/uri", Cfg.class.getResource("/background.png").toURI()).toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    public int backgroundImageNoise() {
        return tecl.integer(BACKGROUNDIMAGE + "/noise", 0);
    }

    public int nextUpCount() {
        return tecl.integer(SCREEN + "/nextUp/count", 3);
    }

}
