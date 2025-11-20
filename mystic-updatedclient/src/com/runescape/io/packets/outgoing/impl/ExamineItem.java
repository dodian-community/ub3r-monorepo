package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ExamineItem implements OutgoingPacket {

	int nodeId;
	
	public ExamineItem(int nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(2);
		buf.putShort(nodeId);
	}
}
