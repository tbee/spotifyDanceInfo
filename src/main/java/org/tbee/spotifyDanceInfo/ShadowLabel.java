package org.tbee.spotifyDanceInfo;

import org.tbee.sway.SEditorPane;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

public class ShadowLabel extends SEditorPane {

    static public ShadowLabel of() {
        return new ShadowLabel();
    }

    public ShadowLabel() {
        super();
        name("dummy");
        opaque(false);
        background(new Color(0,0,0, 100));
        editable(false);
        contentType("text/html");
    }

    @Override
    public void paintComponent(Graphics g) {
        Color foreground = getForeground();
        Color background = getBackground();

        setForeground(background);
        setBackground(foreground);

//        int fontSize = getFont().getSize();
//        int x = fontSize / 20;
//        int y = fontSize / 25;
        int x = 3;
        int y = 2;
        List.of(new offset(x, y), new offset(-x, y), new offset(x, -y), new offset(-x, -y),
                new offset(x, 0), new offset(-x, 0),
                new offset(0, y), new offset(0, -y))
                .forEach(o -> {
                    g.translate(o.x, o.y);
                    super.paintComponent(g);
                    g.translate(-o.x, -o.y);
                });

        setForeground(foreground);
        setBackground(background);
        super.paintComponent(g);
    }
    record offset(int x, int y) {}
}
