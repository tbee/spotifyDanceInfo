package org.tbee.spotifyDanceInfoWeb;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.Session;
import org.tbee.spotifyDanceInfo.Cfg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ScreenData implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenData.class);

    volatile private Song currentlyPlaying = new Song();
    private List<Song> nextUp = new ArrayList<>();
    private String time = "";
    private boolean showTips = false;
    private int maxNumberOfActiveBackgroundTasks = 0;
    private int numberOfActiveBackgroundTasks = 0;
    private int numberOfExceptionsInBackgroundTasks = 0;
    private boolean forceRefresh = false;

    static public ScreenData get(HttpSession session) {
        ScreenData screenData = (ScreenData) session.getAttribute(ScreenData.class.getName());
        if (LOGGER.isDebugEnabled()) LOGGER.debug("ScreenData retrieved from HTTP session " + session.getId() + " -> " + screenData);
        return screenData;
    }
    static public ScreenData get(Session session) {
        ScreenData screenData = session.getAttribute(ScreenData.class.getName());
        if (LOGGER.isDebugEnabled()) LOGGER.debug("ScreenData retrieved from Spring session " + session.getId() + " -> " + screenData);
        return screenData;
    }

    public ScreenData storeIn(HttpSession session) {
        session.setAttribute(ScreenData.class.getName(), this);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("ScreenData stored in HTTP session " + session.getId() + " -> " + this);
        return this;
    }
    public ScreenData storeIn(Session session) {
        session.setAttribute(ScreenData.class.getName(), this);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("ScreenData stored in Spring session " + session.getId() + " -> " + this);
        return this;
    }

    public ScreenData refresh(Cfg cfg) {
        this.numberOfExceptionsInBackgroundTasks = cfg.getNumberOfExceptionsInBackgroundTasks();
        this.numberOfActiveBackgroundTasks = cfg.getNumberOfActiveBackgroundTasks();
        if (numberOfActiveBackgroundTasks > maxNumberOfActiveBackgroundTasks) {
            maxNumberOfActiveBackgroundTasks = numberOfActiveBackgroundTasks;
        }
        forceRefresh = true;
        return this;
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

    public boolean forceRefresh() {
        return forceRefresh;
    }
    public ScreenData forceRefresh(boolean v) {
        this.forceRefresh = v;
        return this;
    }

    public String status() {
        return numberOfActiveBackgroundTasks == 0 ? ""
             : "Loading playlists " + numberOfActiveBackgroundTasks + "/" + maxNumberOfActiveBackgroundTasks
             + (numberOfExceptionsInBackgroundTasks == 0 ? "" : " (" + numberOfExceptionsInBackgroundTasks + " failed)");
    }
}
