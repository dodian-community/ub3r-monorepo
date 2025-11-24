package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class EnterAmount implements OutgoingPacket {

	private int amount;
	public EnterAmount(int amount) {
		this.amount = amount;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(208);
		buf.putInt(amount);
	}

}
