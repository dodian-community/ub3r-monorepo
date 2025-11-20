package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class NpcOption4 implements OutgoingPacket {

	int nodeId;
	
	public NpcOption4(int nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(18);
		buf.writeUnsignedWordBigEndian(nodeId);
	}
}
