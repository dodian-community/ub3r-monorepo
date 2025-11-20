package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class RegionChange implements OutgoingPacket {

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(210);
		buf.putInt(0x3f008edd);
	}
	
}
