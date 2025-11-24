package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class SwitchItemSlot implements OutgoingPacket {

	int interface_;
	int param2;
	int dragFromSlot;	
	int toInterface_;
	
	public SwitchItemSlot(int interface_, int param2, int dragFromSlot, int toInterface_) {
		this.interface_ = interface_;
		this.param2 = param2;
		this.dragFromSlot = dragFromSlot;
		this.toInterface_ = toInterface_;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(214);
		buf.putInt(interface_);
		buf.method424(param2);
		buf.writeSignedBigEndian(dragFromSlot);
		buf.writeUnsignedWordBigEndian(toInterface_);
	}


}
