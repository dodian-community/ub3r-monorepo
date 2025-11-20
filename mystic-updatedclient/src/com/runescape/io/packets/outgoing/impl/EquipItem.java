package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class EquipItem implements OutgoingPacket {

	int slot;
	int interfaceId;
	int nodeId;

	public EquipItem(int nodeId, int slot, int interfaceId) {
		this.slot = slot;
		this.interfaceId = interfaceId;
		this.nodeId = nodeId;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(41);
		buf.putShort(nodeId);
		buf.writeUnsignedWordA(slot);
		buf.writeUnsignedWordA(interfaceId);
	}
}
