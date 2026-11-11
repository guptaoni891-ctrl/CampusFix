package com.campusfix.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/** Small Java2D navigation icons so the interface has no image dependency. */
public final class NavigationIcon implements Icon {
    public enum Type { OVERVIEW, TICKETS, ADD, USERS, ANALYTICS, HISTORY }

    private final Type type;
    private final Color color;

    public NavigationIcon(Type type, Color color) {
        this.type = type;
        this.color = color;
    }

    @Override public int getIconWidth() { return 19; }
    @Override public int getIconHeight() { return 19; }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case OVERVIEW -> {
                g.drawRoundRect(1, 1, 7, 7, 2, 2); g.drawRoundRect(11, 1, 7, 7, 2, 2);
                g.drawRoundRect(1, 11, 7, 7, 2, 2); g.drawRoundRect(11, 11, 7, 7, 2, 2);
            }
            case TICKETS -> {
                g.drawRoundRect(2, 1, 15, 17, 3, 3);
                g.drawLine(6, 6, 14, 6); g.drawLine(6, 10, 14, 10); g.drawLine(6, 14, 11, 14);
            }
            case ADD -> {
                g.drawOval(1, 1, 17, 17); g.drawLine(9, 5, 9, 14); g.drawLine(5, 9, 14, 9);
            }
            case USERS -> {
                g.drawOval(6, 1, 7, 7); g.drawArc(3, 9, 13, 9, 0, 180);
                g.drawOval(1, 5, 5, 5); g.drawOval(13, 5, 5, 5);
            }
            case ANALYTICS -> {
                g.drawLine(2, 17, 17, 17); g.drawLine(2, 17, 2, 3);
                g.fillRoundRect(5, 10, 3, 6, 2, 2); g.fillRoundRect(10, 6, 3, 10, 2, 2);
                g.fillRoundRect(15, 2, 3, 14, 2, 2);
            }
            case HISTORY -> {
                g.drawArc(2, 2, 15, 15, -60, 300);
                Path2D arrow = new Path2D.Double(); arrow.moveTo(2, 2); arrow.lineTo(2, 8); arrow.lineTo(7, 5); arrow.closePath();
                g.fill(arrow); g.drawLine(9, 5, 9, 10); g.drawLine(9, 10, 13, 12);
            }
        }
        g.dispose();
    }
}
