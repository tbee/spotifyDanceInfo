package org.tbee.spotifyDanceInfo;

import java.util.prefs.Preferences;

public class CfgForTest extends Cfg<CfgForTest> {

    public CfgForTest(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        super(configFileName, moreTracksInBackground, generateConfigFileIfNotFound);
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
}
