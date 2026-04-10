// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.io.IOException;
import java.io.RandomAccessFile;

public class FileStore {

	public FileStore(int _type, int _maxSize, RandomAccessFile _data, RandomAccessFile _index) {
		type = _type;
		data = _data;
		index = _index;
		maxSize = _maxSize;
	}

	public synchronized byte[] get(int id) {
		try {
			seek(id * 6, index);
			int l;
			for (int j = 0; j < 6; j += l) {
				l = index.read(tmpBuf, j, 6 - j);
				if (l == -1)
					return null;
			}

			int len = ((tmpBuf[0] & 0xff) << 16) + ((tmpBuf[1] & 0xff) << 8) + (tmpBuf[2] & 0xff);
			int sector = ((tmpBuf[3] & 0xff) << 16) + ((tmpBuf[4] & 0xff) << 8) + (tmpBuf[5] & 0xff);
			if (len < 0 || len > maxSize)
				return null;
			if (sector <= 0 || sector > data.length() / 520L)
				return null;
			byte buf[] = new byte[len];
			int read = 0;
			for (int cycle = 0; read < len; cycle++) {
				if (sector == 0)
					return null;
				seek(sector * 520, data);
				int k = 0;
				int remaining = len - read;
				if (remaining > 512)
					remaining = 512;
				int j2;
				for (; k < remaining + 8; k += j2) {
					j2 = data.read(tmpBuf, k, (remaining + 8) - k);
					if (j2 == -1)
						return null;
				}

				int cur_id = ((tmpBuf[0] & 0xff) << 8) + (tmpBuf[1] & 0xff);
				int cur_cycle = ((tmpBuf[2] & 0xff) << 8) + (tmpBuf[3] & 0xff);
				int next_sector = ((tmpBuf[4] & 0xff) << 16) + ((tmpBuf[5] & 0xff) << 8)
						+ (tmpBuf[6] & 0xff);
				int cur_type = tmpBuf[7] & 0xff;
				if (cur_id != id || cur_cycle != cycle || cur_type != type)
					return null;
				if (next_sector < 0 || next_sector > data.length() / 520L)
					return null;
				for (int pos = 0; pos < remaining; pos++)
					buf[read++] = tmpBuf[pos + 8];

				sector = next_sector;
			}

			return buf;
		} catch (IOException _ex) {
			return null;
		}
	}

	public synchronized boolean put(int len, byte buf[], int id) {
		boolean success = put(buf, id, true, len);
		if (!success)
			success = put(buf, id, false, len);
		return success;
	}

	public synchronized boolean put(byte buf[], int id, boolean overwrite, int len) {
		try {
			int sector;
			if (overwrite) {
				seek(id * 6, index);
				int k1;
				for (int i1 = 0; i1 < 6; i1 += k1) {
					k1 = index.read(tmpBuf, i1, 6 - i1);
					if (k1 == -1)
						return false;
				}

				sector = ((tmpBuf[3] & 0xff) << 16) + ((tmpBuf[4] & 0xff) << 8) + (tmpBuf[5] & 0xff);
				if (sector <= 0 || sector > data.length() / 520L)
					return false;
			} else {
				sector = (int) ((data.length() + 519L) / 520L);
				if (sector == 0)
					sector = 1;
			}
			tmpBuf[0] = (byte) (len >> 16);
			tmpBuf[1] = (byte) (len >> 8);
			tmpBuf[2] = (byte) len;
			tmpBuf[3] = (byte) (sector >> 16);
			tmpBuf[4] = (byte) (sector >> 8);
			tmpBuf[5] = (byte) sector;
			seek(id * 6, index);
			index.write(tmpBuf, 0, 6);
			int written = 0;
			for (int chunk = 0; written < len; chunk++) {
				int nextSector = 0;
				if (overwrite) {
					seek(sector * 520, data);
					int pos;
					int tmp;
					for (pos = 0; pos < 8; pos += tmp) {
						tmp = data.read(tmpBuf, pos, 8 - pos);
						if (tmp == -1)
							break;
					}

					if (pos == 8) {
						int _id = ((tmpBuf[0] & 0xff) << 8) + (tmpBuf[1] & 0xff);
						int _chunk = ((tmpBuf[2] & 0xff) << 8) + (tmpBuf[3] & 0xff);
						nextSector = ((tmpBuf[4] & 0xff) << 16) + ((tmpBuf[5] & 0xff) << 8)
								+ (tmpBuf[6] & 0xff);
						int _type = tmpBuf[7] & 0xff;
						if (_id != id || _chunk != chunk || _type != type)
							return false;
						if (nextSector < 0 || nextSector > data.length() / 520L)
							return false;
					}
				}
				if (nextSector == 0) {
					overwrite = false;
					nextSector = (int) ((data.length() + 519L) / 520L);
					if (nextSector == 0)
						nextSector++;
					if (nextSector == sector)
						nextSector++;
				}
				if (len - written <= 512)
					nextSector = 0;
				tmpBuf[0] = (byte) (id >> 8);
				tmpBuf[1] = (byte) id;
				tmpBuf[2] = (byte) (chunk >> 8);
				tmpBuf[3] = (byte) chunk;
				tmpBuf[4] = (byte) (nextSector >> 16);
				tmpBuf[5] = (byte) (nextSector >> 8);
				tmpBuf[6] = (byte) nextSector;
				tmpBuf[7] = (byte) type;
				seek(sector * 520, data);
				data.write(tmpBuf, 0, 8);
				int sectorLen = len - written;
				if (sectorLen > 512)
					sectorLen = 512;
				data.write(buf, written, sectorLen);
				written += sectorLen;
				sector = nextSector;
			}

			return true;
		} catch (IOException _ex) {
			return false;
		}
	}

	public synchronized void seek(int pos, RandomAccessFile file) throws IOException {
		if (pos < 0 || pos > 0x3c00000) {
			System.out.println("Badseek - pos:" + pos + " len:" + file.length());
			pos = 0x3c00000;
			try {
				Thread.sleep(1000L);
			} catch (Exception _ex) {
			}
		}
		file.seek(pos);
	}
	
	public static byte tmpBuf[] = new byte[520];
	public RandomAccessFile data;
	public RandomAccessFile index;
	public int type;
	public int maxSize;

}
