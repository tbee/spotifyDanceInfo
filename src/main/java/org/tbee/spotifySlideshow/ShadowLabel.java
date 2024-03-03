package org.tbee.spotifySlideshow;

import org.tbee.sway.SLabel;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

public class ShadowLabel extends SLabel {

    public static final List<offset> OFFSETS = List.of(
            new offset(3, 2), new offset(-3, 2), new offset(3, -2), new offset(-3, -2),
            new offset(3, 0), new offset(-3, 0),
            new offset(0, 2), new offset(0, -2));

    public ShadowLabel() {
        super();
    }

    @Override
    public void paintComponent(Graphics g) {
        Color foreground = getForeground();
        Color background = getBackground();

        setForeground(background);
        setBackground(foreground);
        OFFSETS.forEach(o -> {
            g.translate(o.x, o.y);
            super.paintComponent(g);
            g.translate(-1 * o.x, -1 * o.y);
        });

        setForeground(foreground);
        setBackground(background);
        super.paintComponent(g);
    }

    record offset(int x, int y) {}

    static public ShadowLabel of() {
        return new ShadowLabel();
    }
}
