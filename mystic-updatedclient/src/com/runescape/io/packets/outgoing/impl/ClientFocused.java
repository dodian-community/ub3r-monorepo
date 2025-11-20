package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ClientFocused implements OutgoingPacket {

	private boolean focused;
	public ClientFocused(boolean focused) {
		this.focused = focused;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(3);
		buf.putByte(focused ? 1 : 0);
	}


}
