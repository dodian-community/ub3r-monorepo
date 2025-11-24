package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ItemOnPlayer implements OutgoingPacket {

	int anInt1285;
	int nodeId;
	int anInt1283;
	int anInt1284;
	
	public ItemOnPlayer(int anInt1284, int nodeId, int anInt1285, int anInt1283) {
		this.anInt1285 = anInt1285;
		this.nodeId = nodeId;
		this.anInt1283 = anInt1283;
		this.anInt1284 = anInt1284;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(14);
		buf.writeUnsignedWordA(anInt1284);
		buf.putShort(nodeId);
		buf.putShort(anInt1285);
		buf.writeUnsignedWordBigEndian(anInt1283);
	}
}
