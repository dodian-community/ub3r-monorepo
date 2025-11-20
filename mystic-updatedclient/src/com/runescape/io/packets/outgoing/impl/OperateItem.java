package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class OperateItem implements OutgoingPacket {

	private int actionId;
	private int clickId;
	
	public OperateItem(int actionId, int clickId) {
		this.actionId = actionId;
		this.clickId = clickId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(232);
		buf.putInt(actionId);
		buf.putInt(clickId);
	}

}
