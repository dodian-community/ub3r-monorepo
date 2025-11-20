package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class Chat implements OutgoingPacket{

	private int color;
	private int effect;
	private String say;
	public Chat(int color, int effect, String say) {
		this.color = color;
		this.effect = effect;
		this.say = say;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(4);
		buf.putByte(color);
		buf.putByte(effect);
		buf.putString(say);
	}

}
