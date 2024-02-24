package org.tbee.spotifySlideshow;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;

public class ShadowLabel extends JLabel {

    private boolean invertColors = false;

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

    public void setInvertColors(boolean invertColors) {
        this.invertColors = invertColors;
    }
}
