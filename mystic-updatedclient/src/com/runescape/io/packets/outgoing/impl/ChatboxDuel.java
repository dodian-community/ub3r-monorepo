package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ChatboxDuel implements OutgoingPacket {

	int plr;

	public ChatboxDuel(int plr) {
		this.plr = plr;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(128);
		buf.putShort(plr);
	}
}
