package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ExamineStuff implements OutgoingPacket {

	int nodeId, slot, posX, posY;
	
	public ExamineStuff(int slot, int nodeId, int posX, int posY) {
		this.slot = slot;
		this.nodeId = nodeId;
		this.posX = posX;
		this.posY = posY;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(2);
		buf.putShort(slot);
		buf.putInt(posX);
		buf.putShort(nodeId);
		buf.putShort(posY);
	}
}
