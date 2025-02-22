package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {
    volatile private Song currentlyPlaying = new Song();
    private List<Song> nextUp = new ArrayList<>();
    private String time = "";
    private boolean showTips = false;
    private int maxNumberOfActiveBackgroundTasks = 0;
    private int numberOfActiveBackgroundTasks = 0;

    static public ScreenData get(HttpSession session) {
        return (ScreenData) session.getAttribute(ScreenData.class.getName());
    }

    public ScreenData(HttpSession session) {
        session.setAttribute(ScreenData.class.getName(), this);
    }

    public void refresh(int numberOfActiveBackgroundTasks) {
        this.numberOfActiveBackgroundTasks = numberOfActiveBackgroundTasks;
        if (numberOfActiveBackgroundTasks > maxNumberOfActiveBackgroundTasks) {
            maxNumberOfActiveBackgroundTasks = numberOfActiveBackgroundTasks;
        }
        // put a copy in place with deviating trackid, so it will be updated, but the screen data stays the same for now.
        currentlyPlaying = new Song("force refresh", currentlyPlaying.title(), currentlyPlaying.artist());
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

    public String status() {
        return numberOfActiveBackgroundTasks == 0 ? "" : "Loading playlists " + numberOfActiveBackgroundTasks + "/" + maxNumberOfActiveBackgroundTasks;
    }
}
