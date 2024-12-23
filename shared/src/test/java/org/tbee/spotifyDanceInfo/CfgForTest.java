package org.tbee.spotifyDanceInfo;

import java.util.HashMap;
import java.util.Map;

public class CfgForTest extends Cfg<CfgForTest> {

    private Map<String, String> storage = new HashMap<>();

    public CfgForTest(String configFileName, boolean moreTracksInBackground, boolean generateConfigFileIfNotFound) {
        super(configFileName, moreTracksInBackground, generateConfigFileIfNotFound);
    }

    @Override
    public void remember(String id, String v) {
        storage.put(id, v);
    }

    @Override
    public String recall(String id) {
        return storage.getOrDefault(id, "");
    }
}
