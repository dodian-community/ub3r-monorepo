package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class FollowPlayer implements OutgoingPacket {

	int nodeId;

	public FollowPlayer(int nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(73);
		buf.writeUnsignedWordBigEndian(nodeId);
	}
}
