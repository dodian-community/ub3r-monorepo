package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class DropItem implements OutgoingPacket {

	int nodeId;
	int interfaceId;
	int slot;
	
	public DropItem(int nodeId, int interfaceId, int slot) {
		this.nodeId = nodeId;
		this.interfaceId = interfaceId;
		this.slot = slot;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(87);
		buf.writeUnsignedWordA(nodeId);
		buf.putShort(interfaceId);
		buf.writeUnsignedWordA(slot);
	}


}
