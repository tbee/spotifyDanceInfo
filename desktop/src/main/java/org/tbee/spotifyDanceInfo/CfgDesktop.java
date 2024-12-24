package org.tbee.spotifyDanceInfo;

import org.tbee.tecl.TECL;

import java.awt.Font;
import java.util.prefs.Preferences;

public class CfgDesktop extends org.tbee.spotifyDanceInfo.Cfg<CfgDesktop> {

    public CfgDesktop() {
        super();
    }

    public CfgDesktop(boolean generateConfigFileIfNotFound) {
        super(generateConfigFileIfNotFound);
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
}
