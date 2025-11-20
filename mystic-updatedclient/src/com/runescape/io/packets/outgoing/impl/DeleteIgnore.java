package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class DeleteIgnore implements OutgoingPacket {

	private long ignore;
	public DeleteIgnore(long ignore) {
		this.ignore = ignore;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(74);
		buf.putLong(ignore);
	}


}
