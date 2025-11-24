package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ItemContainerOption4 implements OutgoingPacket {

	int slot;
	int interfaceId;
	int nodeId;
	
	public ItemContainerOption4(int slot, int interfaceId, int nodeId) {
		this.nodeId = nodeId;
		this.slot = slot;
		this.interfaceId = interfaceId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(129);
		buf.writeUnsignedWordA(slot);
		buf.putInt(interfaceId);
		buf.writeUnsignedWordA(nodeId);
	}
}
