package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ExamineNpc implements OutgoingPacket {

	int nodeId;
	
	public ExamineNpc(int nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(6);
		buf.putShort(nodeId);
	}
}
