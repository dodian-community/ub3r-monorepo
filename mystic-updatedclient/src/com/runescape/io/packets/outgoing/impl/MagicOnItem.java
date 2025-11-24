package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class MagicOnItem implements OutgoingPacket {

	int slot;
	int nodeId;
	int interfaceId;
	int selectedSpellId;
	
	public MagicOnItem(int slot, int nodeId, int interfaceId, int selectedSpellId) {
		this.slot = slot;
		this.nodeId = nodeId;
		this.interfaceId = interfaceId;
		this.selectedSpellId = selectedSpellId;
	}
	
	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(237);
		buf.putShort(slot);
		buf.writeUnsignedWordA(nodeId);
		buf.putShort(interfaceId);
		buf.writeUnsignedWordA(selectedSpellId);
	}


}
