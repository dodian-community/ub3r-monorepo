package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class Command implements OutgoingPacket {

	private String cmd;
	public Command(String cmd) {
		this.cmd = cmd;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(103);
		buf.putString(cmd);
	}


}
