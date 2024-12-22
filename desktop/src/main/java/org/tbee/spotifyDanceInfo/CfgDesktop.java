package org.tbee.spotifyDanceInfo;

import java.util.prefs.Preferences;

public class CfgDesktop extends org.tbee.spotifyDanceInfo.Cfg<CfgDesktop> {

    public CfgDesktop() {
        super();
    }

    public CfgDesktop(boolean b) {
        super(b);
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
