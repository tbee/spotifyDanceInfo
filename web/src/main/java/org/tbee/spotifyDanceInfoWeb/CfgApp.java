package org.tbee.spotifyDanceInfoWeb;

import org.tbee.spotifyDanceInfo.Cfg;

import java.util.prefs.Preferences;

public class CfgApp extends Cfg<CfgApp> {

    public CfgApp() {
        super();
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
