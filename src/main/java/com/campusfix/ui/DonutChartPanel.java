package com.campusfix.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;

/** Lightweight Java2D donut chart with an integrated legend. */
public final class DonutChartPanel extends JPanel {
    private final Map<String, Long> values;
    private final Color[] colors;

    public DonutChartPanel(Map<String, Long> values, Color... colors) {
        this.values = new LinkedHashMap<>(values);
        this.colors = colors.clone();
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long total = values.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            drawEmpty(g);
            g.dispose();
            return;
        }
        int size = Math.max(90, Math.min(getHeight() - 30, getWidth() / 2));
        int x = 12;
        int y = Math.max(8, (getHeight() - size) / 2);
        double start = 90;
        int index = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            double extent = -360.0 * entry.getValue() / total;
            g.setColor(color(index));
            g.fill(new Arc2D.Double(x, y, size, size, start, extent, Arc2D.PIE));
            start += extent;
            index++;
        }
        int hole = (int) (size * .56);
        g.setColor(Style.WHITE);
        g.fillOval(x + (size - hole) / 2, y + (size - hole) / 2, hole, hole);
        g.setColor(Style.TEXT);
        g.setFont(Style.font(20, true));
        String totalText = String.valueOf(total);
        g.drawString(totalText, x + size / 2 - g.getFontMetrics().stringWidth(totalText) / 2,
            y + size / 2 + 5);
        drawLegend(g, x + size + 18, 23);
        g.dispose();
    }

    private void drawLegend(Graphics2D g, int x, int y) {
        int index = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            g.setColor(color(index));
            g.fillRoundRect(x, y + index * 25, 9, 9, 4, 4);
            g.setColor(Style.TEXT);
            g.setFont(Style.font(10, true));
            g.drawString(entry.getKey() + "  " + entry.getValue(), x + 15, y + 9 + index * 25);
            index++;
        }
    }

    private void drawEmpty(Graphics2D g) {
        int size = Math.min(100, Math.min(getWidth(), getHeight()) - 24);
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g.setStroke(new BasicStroke(13f));
        g.setColor(new Color(225, 233, 238));
        g.drawOval(x, y, size, size);
        g.setColor(Style.MUTED);
        g.setFont(Style.font(11, true));
        String text = "No data";
        g.drawString(text, getWidth() / 2 - g.getFontMetrics().stringWidth(text) / 2,
            getHeight() / 2 + 4);
    }

    private Color color(int index) {
        return colors.length == 0 ? Style.TEAL : colors[index % colors.length];
    }
}
