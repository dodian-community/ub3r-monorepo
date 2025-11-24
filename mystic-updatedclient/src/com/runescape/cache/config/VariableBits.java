package com.runescape.cache.config;

import com.runescape.cache.FileArchive;
import com.runescape.io.Buffer;

public final class VariableBits {
	
	public static VariableBits varbits[];	
	public int setting;
	public int low;
	public int high;
	private boolean aBoolean651;

	public static void init(FileArchive streamLoader) {
		Buffer stream = new Buffer(streamLoader.readFile("varbit.dat"));
		int size = stream.readUShort();

		if (varbits == null) {
			varbits = new VariableBits[size];
		}

		for (int index = 0; index < size; index++) {

			if (varbits[index] == null) {
				varbits[index] = new VariableBits();
			}

			varbits[index].decode(stream);

			if (varbits[index].aBoolean651) {
				VariablePlayer.variables[varbits[index].setting].aBoolean713 = true;
			}

		}

		if (stream.currentPosition != stream.payload.length) {
			System.out.println("varbit load mismatch");
		}

	}

	private void decode(Buffer stream) {
		int opcode = stream.readUnsignedByte();
		if (opcode == 0) {
			return;
		} else if (opcode == 1) {
			setting = stream.readUShort();
			low = stream.readUnsignedByte();
			high = stream.readUnsignedByte();
		} else {
			System.out.println("[VARBIT WARNING] Unknown opcode: " + opcode);
		}
	}

	private VariableBits() {
		aBoolean651 = false;
	}

	public int getSetting() {
		return setting;
	}

	public int getLow() {
		return low;
	}

	public int getHigh() {
		return high;
	}
	
}
