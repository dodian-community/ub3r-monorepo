package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ChatSettings implements OutgoingPacket {
	
	int publicMode; 
	int privateMode;
	int tradeMode;
	
	public ChatSettings(int publicMode, int privateMode, int tradeMode) {
		this.publicMode = publicMode;
		this.privateMode = privateMode;
		this.tradeMode = tradeMode;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(95);
		buf.putByte(publicMode);
		buf.putByte(privateMode);
		buf.putByte(tradeMode);
	}


}
