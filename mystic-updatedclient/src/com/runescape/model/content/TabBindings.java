package com.runescape.model.content;

import java.awt.event.KeyEvent;

import com.runescape.Client;
/**
 * Handles bindings for the gameframe tabs.
 * @author Professor Oak
 *
 */
public class TabBindings {

	public static int tabBindings[] = {
			KeyEvent.VK_F5,
			-1,
			-1,
			KeyEvent.VK_F1,
			KeyEvent.VK_F2,
			KeyEvent.VK_F3,
			KeyEvent.VK_F4,
			KeyEvent.VK_F6,
			KeyEvent.VK_F7,
			KeyEvent.VK_F8,
			KeyEvent.VK_F9,
			KeyEvent.VK_F10,
			KeyEvent.VK_F11,
			KeyEvent.VK_F12,
	};


	public static void restoreDefault() {
		tabBindings = new int[]{
				KeyEvent.VK_F5,
				-1,
				-1,
				KeyEvent.VK_F1,
				KeyEvent.VK_F2,
				KeyEvent.VK_F3,
				KeyEvent.VK_F4,
				KeyEvent.VK_F6,
				KeyEvent.VK_F7,
				KeyEvent.VK_F8,
				KeyEvent.VK_F9,
				KeyEvent.VK_F10,
				KeyEvent.VK_F11,
				KeyEvent.VK_F12,	
		};
	}

	public static void bind(int index, int key) {

		for(int i = 0; i < tabBindings.length; i++) {
			if(key == tabBindings[i]) {
				tabBindings[i] = -1;
			}
		}

		tabBindings[index] = key;
		
		Client.instance.savePlayerData();
	}

	public static boolean isBound(int key) {
		for(int i = 0; i < tabBindings.length; i++) {
			if(key == tabBindings[i]) {
				Client.setTab(i);
				return true;
			}
		}
		return false;
	}
}
