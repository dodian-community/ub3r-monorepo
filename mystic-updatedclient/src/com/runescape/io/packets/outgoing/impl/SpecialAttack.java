package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class SpecialAttack implements OutgoingPacket {

	private int barId;
	
	public SpecialAttack(int barId) {
		this.barId = barId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(184);
		buf.putInt(barId);
	}

}
