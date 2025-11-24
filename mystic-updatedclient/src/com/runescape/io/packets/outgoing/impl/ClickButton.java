package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ClickButton implements OutgoingPacket {

	private int buttonId;
	
	public ClickButton(int button) {
		this.buttonId = button;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(185);
		buf.putInt(buttonId);
	}

}
