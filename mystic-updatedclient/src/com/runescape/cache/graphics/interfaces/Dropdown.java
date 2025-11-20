package com.runescape.cache.graphics.interfaces;

import com.runescape.Client;
import com.runescape.cache.graphics.Widget;

/**
 * Handles all the drop down menu actions.
 *
 * @author Daniel
 */
public enum Dropdown {
	DEFAULT() {
		@Override
		public void selectOption(int option, Client client, Widget rsint) {
			//client.outgoing.putOpcode(255);
			//client.outgoing.putDWordBigEndian(rsint.id);
			//client.outgoing.putByte(option);
			Client.sendString(rsint.disabledMessage, option);
		}
	};

	Dropdown() {
	}

	public abstract void selectOption(int option, Client client, Widget rsint);
}