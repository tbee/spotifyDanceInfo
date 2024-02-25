package org.tbee.spotifySlideshow;

import org.tbee.sway.SLabel;

import java.awt.Color;
import java.awt.Graphics;

public class ShadowLabel extends SLabel {

    public ShadowLabel() {
        super();
    }

    @Override
    public void paintComponent(Graphics g) {
        Color foreground = getForeground();
        Color background = getBackground();

        setForeground(background);
        setBackground(foreground);
        super.paintComponent(g);

        setForeground(foreground);
        setBackground(background);
        g.translate(-3, -2);
        super.paintComponent(g);
    }

    static public ShadowLabel of() {
        return new ShadowLabel();
    }
}
