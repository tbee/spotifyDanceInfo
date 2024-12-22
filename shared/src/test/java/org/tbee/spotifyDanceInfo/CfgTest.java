package org.tbee.spotifyDanceInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CfgTest {

    @Test
    public void example() {
        Cfg cfg = new CfgForTest("example.tecl", false, false);
        Assertions.assertEquals("...", cfg.webapiClientId());
    }

    @Test
    public void tsv() {
        Cfg cfg = new CfgForTest("tsv.tecl", false, false);
        Assertions.assertEquals("[cc]", cfg.trackIdToDanceIds("tsv001").toString());
    }

    @Test
    public void excel() {
        Cfg cfg = new CfgForTest("excel.tecl", false, false);
        Assertions.assertEquals("[ew]", cfg.trackIdToDanceIds("xlsx001").toString());
        Assertions.assertEquals("[ew, ru]", cfg.trackIdToDanceIds("xls002").toString());
        Assertions.assertEquals("[Engelse Wals, Rumba]", cfg.trackIdToDanceIds("xls004").toString());
    }
}
