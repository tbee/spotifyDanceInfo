package org.tbee.spotifyDanceInfoQrks;

import org.tbee.spotifyDanceInfo.Cfg;

import java.util.prefs.Preferences;

public class CfgWeb extends Cfg<CfgWeb> {

    public CfgWeb() {
        super();
    }

    public CfgWeb(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        super(configFileName, moreTracksInBackground, generateConfigFileIfNotFound);
    }

    @Override
    protected void remember(String id, String v) {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        preferences.put(id, v);
    }

    @Override
    protected String recall(String id) {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        return preferences.get(id, "");
    }
}
