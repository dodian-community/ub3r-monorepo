package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class PlayerAttackOption implements OutgoingPacket {

	int nodeId;
	
	public PlayerAttackOption(int nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(153);
		buf.writeUnsignedWordBigEndian(nodeId);
	}
}
