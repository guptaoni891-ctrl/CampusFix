package com.campusfix.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;

/** Responsive horizontal Java2D bar chart for categorical analytics. */
public final class BarChartPanel extends JPanel {
    private final Map<String, Long> values;
    private final Color accent;

    public BarChartPanel(Map<String, Long> values, Color accent) {
        this.values = new LinkedHashMap<>(values);
        this.accent = accent;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long maximum = values.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (maximum == 0) {
            g.setColor(Style.MUTED);
            g.setFont(Style.font(11, true));
            g.drawString("No data for this selection", 14, Math.max(22, getHeight() / 2));
            g.dispose();
            return;
        }
        int count = Math.max(1, values.size());
        int rowHeight = Math.max(24, (getHeight() - 14) / count);
        int labelWidth = Math.min(115, Math.max(70, getWidth() / 3));
        int barWidth = Math.max(30, getWidth() - labelWidth - 42);
        int index = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            int y = 8 + index * rowHeight;
            g.setFont(Style.font(9, true));
            g.setColor(Style.MUTED);
            String label = fit(entry.getKey(), g, labelWidth - 8);
            g.drawString(label, 5, y + 14);
            g.setColor(new Color(230, 236, 240));
            g.fillRoundRect(labelWidth, y + 5, barWidth, 11, 10, 10);
            int filled = (int) Math.round(barWidth * entry.getValue() / (double) maximum);
            g.setColor(accent);
            g.fillRoundRect(labelWidth, y + 5, Math.max(entry.getValue() == 0 ? 0 : 3, filled), 11, 10, 10);
            g.setColor(Style.TEXT);
            g.drawString(String.valueOf(entry.getValue()), labelWidth + barWidth + 8, y + 14);
            index++;
        }
        g.dispose();
    }

    private static String fit(String value, Graphics2D g, int width) {
        if (g.getFontMetrics().stringWidth(value) <= width) return value;
        String result = value;
        while (result.length() > 2 && g.getFontMetrics().stringWidth(result + "…") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }
}
