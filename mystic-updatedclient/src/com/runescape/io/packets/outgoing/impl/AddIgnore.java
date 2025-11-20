package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class AddIgnore implements OutgoingPacket {

	private long friend;
	public AddIgnore(long ignore) {
		this.friend = ignore;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(133);
		buf.putLong(friend);
	}


}
