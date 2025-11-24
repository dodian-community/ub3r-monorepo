package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ItemContainerOption2 implements OutgoingPacket {

	int slot;
	int interfaceId;
	int nodeId;

	public ItemContainerOption2(int interfaceId, int nodeId, int slot) {
		this.slot = slot;
		this.interfaceId = interfaceId;
		this.nodeId = nodeId;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(117);
		buf.putInt(interfaceId);
		buf.writeSignedBigEndian(nodeId);
		buf.writeUnsignedWordBigEndian(slot);
	}
}
