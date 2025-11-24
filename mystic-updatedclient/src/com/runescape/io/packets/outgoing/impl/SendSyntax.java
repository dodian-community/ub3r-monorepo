package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class SendSyntax implements OutgoingPacket {

	private String chat;
	public SendSyntax(String chat) {
		this.chat = chat;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(60);
		buf.putString(chat);
	}


}
