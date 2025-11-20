package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class UpdatePlane implements OutgoingPacket {

	int plane;

	public UpdatePlane(int plane) {
		this.plane = plane;	
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(229);
		buf.putByte(plane);
	}
}
