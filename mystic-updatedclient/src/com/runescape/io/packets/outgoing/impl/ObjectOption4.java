package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ObjectOption4 implements OutgoingPacket {

	int id;
	int val1;
	int val2;
	public ObjectOption4(int val1, int id, int val2) {
		this.id = id;
		this.val1 = val1;
		this.val2 = val2;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(234);
		buf.writeSignedBigEndian(val1);
		buf.writeUnsignedWordA(id);
		buf.writeSignedBigEndian(val2);
	}
}
