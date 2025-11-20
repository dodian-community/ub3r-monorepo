package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class NpcOption3 implements OutgoingPacket {

	int nodeId;
	
	public NpcOption3(int nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(21);
		buf.putShort(nodeId);
	}
}
