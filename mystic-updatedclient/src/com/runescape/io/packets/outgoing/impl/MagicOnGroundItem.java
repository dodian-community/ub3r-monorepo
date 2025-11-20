package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class MagicOnGroundItem implements OutgoingPacket {

	int val1;
	int nodeId;
	int val2;
	int selectedSpellId;
	
	public MagicOnGroundItem(int val1, int nodeId, int val2, int selectedSpellId) {
		this.val1 = val1;
		this.nodeId = nodeId;
		this.val2 = val2;
		this.selectedSpellId = selectedSpellId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(181);
		buf.writeUnsignedWordBigEndian(val1);
		buf.putShort(nodeId);
		buf.writeUnsignedWordBigEndian(val2);
		buf.writeUnsignedWordA(selectedSpellId);
	}


}
