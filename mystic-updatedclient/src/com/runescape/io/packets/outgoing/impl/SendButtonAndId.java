package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class SendButtonAndId implements OutgoingPacket {

	private int buttonId;
	private int itemId;
	
	public SendButtonAndId(int button, int itemId) {
		this.buttonId = button;
		this.itemId = itemId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(201);
		buf.putInt(buttonId);
		buf.putInt(itemId);
	}

}
