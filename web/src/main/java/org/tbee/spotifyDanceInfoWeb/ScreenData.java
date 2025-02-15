package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {
    private Song currentlyPlaying = new Song();
    private List<Song> nextUp = new ArrayList<>();
    private String time = "";
    private boolean showTips = false;

    /**
     * This method can only be called inside a request thread
     */
    static public ScreenData get() {
        return get(SpringUtil.getSession());
    }
    static public ScreenData get(HttpSession session) {
        ScreenData me = (ScreenData) session.getAttribute(ScreenData.class.getName());
        if (me == null) {
            me = new ScreenData(session);
        }
        return me;
    }

    public ScreenData() {
        this(SpringUtil.getSession());
    }

    public ScreenData(HttpSession session) {
        session.setAttribute(ScreenData.class.getName(), this);
    }

    public void refresh() {
        currentlyPlaying = new Song();
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
