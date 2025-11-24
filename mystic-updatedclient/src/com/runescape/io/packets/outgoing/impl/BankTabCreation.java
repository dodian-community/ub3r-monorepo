package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class BankTabCreation implements OutgoingPacket {

	int interface_;
	int dragFromSlot;	
	int toInterface_;
	
	public BankTabCreation(int interface_, int dragFromSlot, int toInterface_) {
		this.interface_ = interface_;
		this.dragFromSlot = dragFromSlot;
		this.toInterface_ = toInterface_;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(216);
		buf.putInt(interface_);
		buf.putShort(dragFromSlot);
		buf.writeUnsignedWordBigEndian(toInterface_);
	}


}
