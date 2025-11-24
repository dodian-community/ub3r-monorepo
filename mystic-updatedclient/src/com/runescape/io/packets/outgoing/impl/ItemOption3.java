package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ItemOption3 implements OutgoingPacket {

	int slot;
	int interfaceId;
	int nodeId;

	public ItemOption3(int nodeId, int slot, int interfaceId) {
		this.slot = slot;
		this.interfaceId = interfaceId;
		this.nodeId = nodeId;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(16);
		buf.writeUnsignedWordA(nodeId);
		buf.writeSignedBigEndian(slot);
		buf.writeSignedBigEndian(interfaceId);
	}
}
