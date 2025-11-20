package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ObjectOption1 implements OutgoingPacket {

	int id;
	int val1;
	int val2;
	public ObjectOption1(int val1, int id, int val2) {
		this.id = id;
		this.val1 = val1;
		this.val2 = val2;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(132);
		buf.writeSignedBigEndian(val1);
		buf.putShort(id);
		buf.writeUnsignedWordA(val2);
	}
}
