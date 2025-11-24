package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class NextDialogue implements OutgoingPacket {

	int interfaceId;
	
	public NextDialogue(int interfaceId) {
		this.interfaceId = interfaceId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(40);
		buf.putShort(interfaceId);
	}


}
