package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class PlayerOption1 implements OutgoingPacket {

	int nodeId;
	
	public PlayerOption1(int nodeId) {
		this.nodeId = nodeId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(128);
		buf.putShort(nodeId);
	}
}
