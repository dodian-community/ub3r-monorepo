package com.fox.components;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.fox.Settings;
import com.fox.listeners.ButtonListener;
import com.fox.utils.Utils;

@SuppressWarnings("serial")
public class MenuBar extends JPanel {
	
	public MenuBar(AppFrame frame) {
		setLayout(null);
		
		setBounds(0, 0, AppFrame.appWidth, 25);
		setBackground(new Color(156, 129, 88));
		
		Control exit = new Control("X");
		exit.setBackground(Settings.primaryColor);
		exit.addActionListener(new ButtonListener());
		exit.setBounds(AppFrame.appWidth - 50, 0, 50, 25);
		add(exit); 
		
		Control mini = new Control("_");
		mini.setBackground(Settings.backgroundColor);
		mini.addActionListener(new ButtonListener());
		mini.setBounds(AppFrame.appWidth - 100, 0, 50, 25);
		add(mini);
		
		JLabel title = new JLabel(""+Settings.SERVER_NAME+" Client Launcher - V"+Settings.LAUNCHER_VERSION+"");
		title.setForeground(Color.WHITE);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		Utils.setFont(title, "OpenSans-Regular.ttf", 14);
		title.setBounds(0, 0, AppFrame.appWidth, 25);
		
		JLabel icon = new JLabel(Utils.getImage("favicon.png"));
		icon.setBounds(3, -2, 24, 28);
		add(icon);
		add(title);
	}
	
}
