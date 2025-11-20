package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class AddFriend implements OutgoingPacket {

	private long friend;
	public AddFriend(long friend) {
		this.friend = friend;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(188);
		buf.putLong(friend);
	}


}
