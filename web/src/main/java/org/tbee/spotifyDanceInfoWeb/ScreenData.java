package org.tbee.spotifyDanceInfoWeb;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {
    private Song currentlyPlaying = new Song();
    private List<Song> nextUp = new ArrayList<>();
    private String time = "";
    private boolean showTips = false;

    static public ScreenData get() {
        ScreenData me = (ScreenData) SpringUtil.getRequest().getSession().getAttribute(ScreenData.class.getName());
        if (me == null) {
            me = new ScreenData();
        }
        return me;
    }
    public ScreenData() {
        SpringUtil.getRequest().getSession().setAttribute(ScreenData.class.getName(), this);
    }

    public Song currentlyPlaying() {
        return currentlyPlaying;
    }
    public ScreenData currentlyPlaying(Song v) {
        this.currentlyPlaying = v;
        return this;
    }

    public List<Song> nextUp() {
        return nextUp;
    }
    public ScreenData nextUp(List<Song> v) {
        nextUp = v;
        return this;
    }

    public String time() {
        return time;
    }
    public ScreenData time(String v) {
        this.time = v;
        return this;
    }

    public boolean showTips() {
        return showTips;
    }
    public ScreenData showTips(boolean v) {
        this.showTips = v;
        return this;
    }

}
