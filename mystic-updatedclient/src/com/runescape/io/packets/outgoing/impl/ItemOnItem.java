package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ItemOnItem implements OutgoingPacket {

	int slot;
	int nodeId;
	int interfaceId;
	int anInt1285;
	int anInt1283;
	int anInt1284;

	
	public ItemOnItem(int slot, int anInt1283, int nodeId, int anInt1284, int anInt1285, int interfaceId) {
		this.nodeId = nodeId;
		this.slot = slot;
		this.interfaceId = interfaceId;
		this.anInt1283 = anInt1283;
		this.anInt1284 = anInt1284;
		this.anInt1285 = anInt1285;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(53);
		buf.putShort(slot);
		buf.writeUnsignedWordA(anInt1283);
		buf.writeSignedBigEndian(nodeId);
		buf.putShort(anInt1284);
		buf.writeUnsignedWordBigEndian(anInt1285);
		buf.putShort(interfaceId);
	}
}
