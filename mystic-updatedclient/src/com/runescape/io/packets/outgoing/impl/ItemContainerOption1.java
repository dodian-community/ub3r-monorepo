package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ItemContainerOption1 implements OutgoingPacket {

	int val1;
	int val2;
	int nodeId;

	public ItemContainerOption1(int val1, int val2, int nodeId) {
		this.val1 = val1;
		this.val2 = val2;
		this.nodeId = nodeId;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(145);
		buf.putInt(val1);
		buf.writeUnsignedWordA(val2);
		buf.writeUnsignedWordA(nodeId);
	}
}
