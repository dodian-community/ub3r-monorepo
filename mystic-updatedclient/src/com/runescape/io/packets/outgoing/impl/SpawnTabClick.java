package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class SpawnTabClick implements OutgoingPacket {

	private int item;
	private boolean spawnX;
	private boolean toBank;
	
	public SpawnTabClick(int item, boolean spawnX, boolean toBank) {
		this.item = item;
		this.spawnX = spawnX;
		this.toBank = toBank;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(187);
		buf.putInt(item);
		buf.putByte(spawnX ? 1 : 0);
		buf.putByte(toBank ? 1 : 0);
	}

}
