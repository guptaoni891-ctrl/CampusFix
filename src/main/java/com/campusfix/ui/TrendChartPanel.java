package com.campusfix.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/** Java2D daily complaint creation trend with scalable axes. */
public final class TrendChartPanel extends JPanel {
    private final List<Map.Entry<LocalDate, Long>> points;
    private final DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM");

    public TrendChartPanel(Map<LocalDate, Long> values) {
        points = new ArrayList<>(values.entrySet());
        points.sort(Map.Entry.comparingByKey());
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (points.isEmpty()) {
            g.setColor(Style.MUTED);
            g.setFont(Style.font(11, true));
            g.drawString("No complaint creation data for this selection", 18, getHeight() / 2);
            g.dispose();
            return;
        }
        int left = 40;
        int right = 22;
        int top = 18;
        int bottom = 32;
        int width = Math.max(10, getWidth() - left - right);
        int height = Math.max(10, getHeight() - top - bottom);
        long maximum = points.stream().map(Map.Entry::getValue).max(Comparator.naturalOrder()).orElse(1L);
        g.setColor(new Color(222, 230, 235));
        g.drawLine(left, top + height, left + width, top + height);
        g.drawLine(left, top, left, top + height);
        g.setColor(Style.TEAL);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int previousX = -1;
        int previousY = -1;
        for (int index = 0; index < points.size(); index++) {
            int x = points.size() == 1 ? left + width / 2
                : left + index * width / (points.size() - 1);
            int y = top + height - (int) Math.round(points.get(index).getValue() * height / (double) maximum);
            if (previousX >= 0) g.drawLine(previousX, previousY, x, y);
            g.fillOval(x - 4, y - 4, 8, 8);
            previousX = x;
            previousY = y;
        }
        g.setColor(Style.MUTED);
        g.setFont(Style.font(9, false));
        String first = date.format(points.get(0).getKey());
        String last = date.format(points.get(points.size() - 1).getKey());
        g.drawString(first, left, getHeight() - 8);
        g.drawString(last, left + width - g.getFontMetrics().stringWidth(last), getHeight() - 8);
        g.drawString(String.valueOf(maximum), 10, top + 4);
        g.drawString("0", 25, top + height + 3);
        g.dispose();
    }
}
