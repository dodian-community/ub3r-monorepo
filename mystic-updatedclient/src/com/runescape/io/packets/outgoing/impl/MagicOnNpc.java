package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class MagicOnNpc implements OutgoingPacket {

	int nodeId;
	int selectedSpellId;
	
	public MagicOnNpc(int nodeId, int selectedSpellId) {
		this.nodeId = nodeId;
		this.selectedSpellId = selectedSpellId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(131);
		buf.writeSignedBigEndian(nodeId);
		buf.writeUnsignedWordA(selectedSpellId);
	}


}
