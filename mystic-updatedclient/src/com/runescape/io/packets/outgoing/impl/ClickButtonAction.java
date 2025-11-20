package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ClickButtonAction implements OutgoingPacket {

	private int buttonId, action;
	
	public ClickButtonAction(int button, int action) {
		this.buttonId = button;
		this.action = action;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(186);
		buf.putInt(buttonId);
		buf.putByte(action);
	}

}
