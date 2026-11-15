package com.campusfix.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Lightweight rounded surface used for cards and forms. */
public class RoundedPanel extends JPanel {
    private final int radius;
    private boolean shadow;

    public RoundedPanel(int radius) {
        this(null, radius);
    }

    public RoundedPanel(LayoutManager layout, int radius) {
        super(layout);
        this.radius = radius;
        setOpaque(false);
        setBackground(Style.WHITE);
    }

    public RoundedPanel withShadow() {
        shadow = true;
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int bottomInset = shadow ? 4 : 0;
        if (shadow) {
            g.setColor(new Color(28, 53, 72, 18));
            g.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 4, radius, radius);
        }
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1 - bottomInset, radius, radius);
        g.dispose();
        super.paintComponent(graphics);
    }
}
