package com.runescape.io.packets.outgoing.impl;

import com.runescape.io.ByteBuffer;
import com.runescape.io.packets.outgoing.OutgoingPacket;

public class ChangeAppearance implements OutgoingPacket {

	boolean isMale; 
	int[] myAppearance;
	int[] characterDesignColours;
	
	public ChangeAppearance(boolean isMale, int[] myAppearance, int[] characterDesignColours) {
		this.isMale = isMale;
		this.myAppearance = myAppearance;
		this.characterDesignColours = characterDesignColours;
	}

	@Override
	public void buildPacket(ByteBuffer buf) {
		buf.putOpcode(11);
		buf.putByte(isMale ? 0 : 1);
		for (int i1 = 0; i1 < 7; i1++)
			buf.putByte(myAppearance[i1]);

		for (int l1 = 0; l1 < 5; l1++)
			buf.putByte(characterDesignColours[l1]);
	}
}
