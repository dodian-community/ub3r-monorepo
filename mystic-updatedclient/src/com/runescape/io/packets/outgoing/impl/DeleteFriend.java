package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class DeleteFriend implements OutgoingPacket {

	private long friend;
	public DeleteFriend(long friend) {
		this.friend = friend;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(215);
		buf.putLong(friend);
	}


}
