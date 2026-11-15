package com.campusfix.ui;

import com.campusfix.model.Status;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;
import javax.swing.JPanel;

/** Compact Java2D representation of the complaint workflow. */
public final class StatusProgressPanel extends JPanel {
    private final Status current;

    public StatusProgressPanel(Status current) {
        this.current = current;
        setOpaque(false);
        setPreferredSize(new Dimension(480, 72));
        setMinimumSize(new Dimension(320, 72));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 24;
        int right = getWidth() - 24;
        int lineY = 24;
        int step = (right - left) / 3;
        g.setStroke(new java.awt.BasicStroke(3f));
        g.setColor(new java.awt.Color(224, 232, 237));
        g.drawLine(left, lineY, right, lineY);
        g.setColor(Style.TEAL);
        g.drawLine(left, lineY, left + step * current.ordinal(), lineY);
        Status[] statuses = Status.values();
        for (int index = 0; index < statuses.length; index++) {
            int x = left + step * index;
            boolean complete = index <= current.ordinal();
            g.setColor(complete ? Style.TEAL : new java.awt.Color(214, 224, 231));
            g.fillOval(x - 8, lineY - 8, 16, 16);
            if (complete) {
                g.setColor(Style.WHITE);
                g.fillOval(x - 3, lineY - 3, 6, 6);
            }
            String text = friendly(statuses[index]);
            g.setFont(Style.font(9, complete));
            g.setColor(complete ? Style.TEXT : Style.MUTED);
            int width = g.getFontMetrics().stringWidth(text);
            g.drawString(text, Math.max(0, Math.min(getWidth() - width, x - width / 2)), 54);
        }
        g.dispose();
    }

    private static String friendly(Status status) {
        String lower = status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
