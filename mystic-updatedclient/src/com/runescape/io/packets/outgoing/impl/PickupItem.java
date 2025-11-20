package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class PickupItem implements OutgoingPacket {

	int nodeId;
	int val1;
	int val2;
	
	public PickupItem(int val1, int nodeId, int val2) {
		this.val1 = val1;
		this.nodeId = nodeId;
		this.val2 = val2;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(236);
		buf.writeUnsignedWordBigEndian(val1);
		buf.putShort(nodeId);
		buf.writeUnsignedWordBigEndian(val2);
	}
}
