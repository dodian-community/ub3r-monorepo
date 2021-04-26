package com.fox.components;

import com.fox.Launcher;
import com.fox.Settings;
import com.fox.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.LineMetrics;

@SuppressWarnings("serial")
public class IconLabel extends JLabel implements MouseListener {

    private int tracking;
    private String action;

    public IconLabel(String text, int fontSize) {
        super(text);
        this.tracking = 1;
        setForeground(Settings.primaryColor);
        this.setRightShadow(1, 1, Settings.iconShadow);
        Utils.setFont(this, "OpenSans-Light.ttf", fontSize);
    }

    public IconLabel(String text, String action, int fontSize) {
        super(text);
        this.tracking = 1;
        this.action = action;
        setForeground(Settings.primaryColor);
        addMouseListener(this);
        this.setRightShadow(1, 1, Settings.iconShadow);
        Utils.setFont(this, "FontAwesome.ttf", fontSize);
    }

    public IconLabel(String text, String action, String font, int fontSize) {
        super(text);
        this.tracking = 1;
        this.action = action;
        setForeground(Settings.primaryColor);
        addMouseListener(this);
        this.setRightShadow(1, 1, Settings.iconShadow);
        Utils.setFont(this, font, fontSize);
    }

    private int left_x, left_y, right_x, right_y;
    private Color left_color, right_color;

    public void setLeftShadow(int x, int y, Color color) {
        left_x = x;
        left_y = y;
        left_color = color;
    }

    public void setRightShadow(int x, int y, Color color) {
        right_x = x;
        right_y = y;
        right_color = color;
    }

    public Dimension getPreferredSize() {
        String text = getText();
        FontMetrics fm = this.getFontMetrics(getFont());

        int w = fm.stringWidth(text);
        w += (text.length() - 1) * tracking;
        w += left_x + right_x;

        int h = fm.getHeight();
        h += left_y + right_y;

        return new Dimension(w, h);
    }

    public void paintComponent(Graphics g) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        char[] chars = getText().toCharArray();

        FontMetrics fm = this.getFontMetrics(getFont());
        int h = fm.getAscent();
        @SuppressWarnings("unused")
        LineMetrics lm = fm.getLineMetrics(getText(), g);
        g.setFont(getFont());

        int x = 0;

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            int w = fm.charWidth(ch) + tracking;

            g.setColor(left_color);
            g.drawString("" + chars[i], x - left_x, h - left_y);

            g.setColor(right_color);
            g.drawString("" + chars[i], x + right_x, h + right_y);

            g.setColor(getForeground());
            g.drawString("" + chars[i], x, h);

            x += w;
        }

    }

    @Override
    public void mouseClicked(MouseEvent me) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseEntered(MouseEvent me) {
        if (this.action != "Music" && this.action != "Play Music")
            AppFrame.tooltip.setText(this.action);
        setForeground(this.getForeground().darker());
    }

    @Override
    public void mouseExited(MouseEvent me) {
        AppFrame.tooltip.setText("");
        setForeground(this.getForeground().brighter());

    }

    @Override
    public void mousePressed(MouseEvent me) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        switch (this.action) {
            case "Community":
                Utils.openWebpage(Settings.community);
                break;
            case "Leaderboards":
                Utils.openWebpage(Settings.leaders);
                break;
            case "Drop list":
                Utils.openWebpage(Settings.store);
                break;
            case "Vote for Us":
                Utils.openWebpage(Settings.vote);
                break;
            case "Report an Issue":
                Utils.openWebpage(Settings.bugs);
                break;
            case "Youtube":
                Utils.openWebpage(Settings.youtube);
                break;
            case "Twitter":
                Utils.openWebpage(Settings.twitter);
                break;
            case "Facebook":
                Utils.openWebpage(Settings.facebook);
                break;
            case "Music":
                Launcher.audio.stopAudio();
                this.setText("\uf204");
                this.action = "Play Music";
                break;
            case "Play Music":
                this.setText("\uf205");
                this.action = "Music";
                Launcher.audio.playAudio();
                break;
        }
    }

    public String getAction() {
        return action;
    }
}