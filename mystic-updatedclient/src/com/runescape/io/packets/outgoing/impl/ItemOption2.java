package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ItemOption2 implements OutgoingPacket {

	int slot;
	int interfaceId;
	int nodeId;

	public ItemOption2(int interfaceId, int slot, int nodeId) {
		this.slot = slot;
		this.interfaceId = interfaceId;
		this.nodeId = nodeId;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(75);
		buf.writeSignedBigEndian(interfaceId);
		buf.writeUnsignedWordBigEndian(slot);
		buf.writeUnsignedWordA(nodeId);
	}
}
