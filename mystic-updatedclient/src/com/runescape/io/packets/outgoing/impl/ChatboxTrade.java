package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ChatboxTrade implements OutgoingPacket {

	int plr;

	public ChatboxTrade(int plr) {
		this.plr = plr;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(139);
		buf.writeUnsignedWordBigEndian(plr);
	}
}
