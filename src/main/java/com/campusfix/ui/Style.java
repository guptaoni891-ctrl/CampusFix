package com.campusfix.ui;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
public final class Style {
    private Style(){}
    public static final Color BG=new Color(247,249,252), WHITE=Color.WHITE, NAVY=new Color(25,42,68), TEXT=new Color(27,40,62), MUTED=new Color(104,119,141), BLUE=new Color(52,108,216), BORDER=new Color(224,231,239), GREEN=new Color(20,150,104), LIGHT_BLUE=new Color(235,242,255);
    public static Font font(int size,boolean bold){return new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,size);}
    public static JButton button(String label){JButton b=new JButton(label);b.setFont(font(13,true));b.setFocusPainted(false);b.setBackground(BLUE);b.setForeground(WHITE);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setBorder(new EmptyBorder(10,15,10,15));return b;}
    public static JButton secondary(String label){JButton b=button(label);b.setBackground(WHITE);b.setForeground(TEXT);b.setBorder(new CompoundBorder(new LineBorder(BORDER),new EmptyBorder(9,14,9,14)));return b;}
    public static JLabel label(String s,int size,boolean bold){JLabel l=new JLabel(s);l.setFont(font(size,bold));l.setForeground(TEXT);return l;}
    public static JPanel panel(){JPanel p=new JPanel();p.setBackground(WHITE);return p;}
}
