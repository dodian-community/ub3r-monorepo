// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class Archive {

	public Archive(byte _data[]) {
		init(_data);
	}

	public void init(byte _data[]) {
		JagBuffer buf = new JagBuffer(_data);
		int extractedSize = buf.getTriByte();
		int size = buf.getTriByte();
		if (size != extractedSize) {
			byte extractedBuf[] = new byte[extractedSize];
			Bzip2.decompress(extractedBuf, extractedSize, _data, size, 6);
			data = extractedBuf;
			buf = new JagBuffer(data);
			extracted = true;
		} else {
			data = _data;
			extracted = false;
		}
		entries = buf.getShort();
		hashes = new int[entries];
		extractedSizes = new int[entries];
		sizes = new int[entries];
		offsets = new int[entries];
		int offset = buf.position + entries * 10;
		for (int pos = 0; pos < entries; pos++) {
			hashes[pos] = buf.getInt();
			extractedSizes[pos] = buf.getTriByte();
			sizes[pos] = buf.getTriByte();
			offsets[pos] = offset;
			offset += sizes[pos];
		}
	}

	public byte[] get(String name) {
		byte[] dest = null;
		int hash = 0;
		name = name.toUpperCase();

		for (int pos = 0; pos < name.length(); pos++)
			hash = (hash * 61 + name.charAt(pos)) - 32;

		for (int entry = 0; entry < entries; entry++) {
			if (hashes[entry] == hash) {
				if (dest == null)
					dest = new byte[extractedSizes[entry]];

				if (!extracted) {
					Bzip2.decompress(dest, extractedSizes[entry], data, sizes[entry], offsets[entry]);
				} else {
					for (int pos = 0; pos < extractedSizes[entry]; pos++)
						dest[pos] = data[offsets[entry] + pos];

				}
				return dest;
			}
		}
		return null;
	}
	
	public byte data[];
	public int entries;
	public int hashes[];
	public int extractedSizes[];
	public int sizes[];
	public int offsets[];
	public boolean extracted;
}
