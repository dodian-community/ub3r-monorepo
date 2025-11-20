package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class AttackNpc implements OutgoingPacket {

	int nodeId;
	
	public AttackNpc(int nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(72);
		buf.writeUnsignedWordA(nodeId);
	}
}
