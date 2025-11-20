package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class PrivateMessage implements OutgoingPacket {

	private long friend;
	private String msg;
	public PrivateMessage(long friend, String msg) {
		this.friend = friend;
		this.msg = msg;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(126);
		buf.putLong(friend);
		buf.putString(msg);
	}


}
