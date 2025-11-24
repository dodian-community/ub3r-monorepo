package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class TradePlayer implements OutgoingPacket {

	int nodeId;

	public TradePlayer(int nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(139);
		buf.writeUnsignedWordBigEndian(nodeId);
	}
}
