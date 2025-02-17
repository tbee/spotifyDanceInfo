package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {
    private Song currentlyPlaying = new Song();
    private List<Song> nextUp = new ArrayList<>();
    private String time = "";
    private boolean showTips = false;

    static public ScreenData get(HttpSession session) {
        return (ScreenData) session.getAttribute(ScreenData.class.getName());
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
