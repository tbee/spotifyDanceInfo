package org.tbee.spotifyDanceInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CfgTest {

    @Test
    public void example() {
        Cfg cfg = new Cfg("example.tecl", false);
        Assertions.assertEquals("...", cfg.webapiClientId());
    }

    @Test
    public void tsv() {
        Cfg cfg = new Cfg("tsv.tecl", false);
        Assertions.assertEquals("[cc]", cfg.trackIdToDanceIds("tsv001").toString());
    }
}
