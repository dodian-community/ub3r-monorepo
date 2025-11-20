package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class MagicOnPlayer implements OutgoingPacket {

	int nodeId;
	int selectedSpellId;
	
	public MagicOnPlayer(int nodeId, int selectedSpellId) {
		this.nodeId = nodeId;
		this.selectedSpellId = selectedSpellId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(249);
		buf.writeUnsignedWordA(nodeId);
		buf.writeUnsignedWordBigEndian(selectedSpellId);
	}


}
