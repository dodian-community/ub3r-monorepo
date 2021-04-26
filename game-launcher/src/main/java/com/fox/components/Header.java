package com.fox.components;

import com.fox.utils.Utils;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class Header extends JLabel {

    public Header(String text) {
        super(text);
        setForeground(Color.WHITE);
        setHorizontalAlignment(SwingConstants.CENTER);
        Utils.setFont(this, "OpenSans-Light.ttf", 32);
    }

}
