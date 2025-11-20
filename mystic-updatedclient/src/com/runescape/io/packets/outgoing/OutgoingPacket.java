package com.runescape.io.packets.outgoing;

import com.runescape.io.ByteBuffer;

public interface OutgoingPacket {

	public void buildPacket(ByteBuffer buf);
	
}
