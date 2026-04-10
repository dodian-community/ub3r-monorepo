// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

import sign.signlink;

public class OnDemandFetcher extends ModelProvider implements Runnable {

	public void waitForResponse() {
		try {
			int available = in.available();
			if (remaining == 0 && available >= 6) {
				aBoolean1338 = true;
				for (int read = 0; read < 6; read += in.read(tmpBuf, read, 6 - read));
				int type = tmpBuf[0] & 0xff;
				int id = ((tmpBuf[1] & 0xff) << 8) + (tmpBuf[2] & 0xff);
				int size = ((tmpBuf[3] & 0xff) << 8) + (tmpBuf[4] & 0xff);
				int chunk = tmpBuf[5] & 0xff;
				currentFile = null;
				for (FileNode file = (FileNode) aClass6_1374.first(); file != null; file = (FileNode) aClass6_1374.next()) {
					if (file.type == type && file.id == id)
						currentFile = file;
					if (currentFile != null)
						file.cycles = 0;
				}

				if (currentFile != null) {
					anInt1353 = 0;
					if (size == 0) {
						signlink.reporterror("Rej: " + type + "," + id);
						currentFile.buf = null;
						if (currentFile.immediate)
							synchronized (completed) {
								completed.addLast(currentFile);
							}
						else
							currentFile.unlink();
						currentFile = null;
					} else {
						if (currentFile.buf == null && chunk == 0)
							currentFile.buf = new byte[size];
						if (currentFile.buf == null && chunk != 0)
							throw new IOException("missing start of file");
					}
				}
				offset = chunk * 500;
				remaining = 500;
				if (remaining > size - chunk * 500)
					remaining = size - chunk * 500;
			}
			if (remaining > 0 && available >= remaining) {
				aBoolean1338 = true;
				byte abyte0[] = tmpBuf;
				int l = 0;
				if (currentFile != null) {
					abyte0 = currentFile.buf;
					l = offset;
				}
				for (int j1 = 0; j1 < remaining; j1 += in.read(abyte0, j1 + l, remaining - j1));
				if (remaining + offset >= abyte0.length && currentFile != null) {
					if (_client.stores[0] != null)
						_client.stores[currentFile.type + 1].put(abyte0.length, abyte0,
								currentFile.id);
					if (!currentFile.immediate && currentFile.type == 3) {
						currentFile.immediate = true;
						currentFile.type = 93;
					}
					if (currentFile.immediate)
						synchronized (completed) {
							completed.addLast(currentFile);
						}
					else
						currentFile.unlink();
				}
				remaining = 0;
				return;
			}
		} catch (IOException ioexception) {
			try {
				aSocket1355.close();
			} catch (Exception _ex) {
			}
			aSocket1355 = null;
			in = null;
			anOutputStream1349 = null;
			remaining = 0;
		}
	}

	public int method325(int i, int j) {
		while (j >= 0)
			return anInt1367;
		return aByteArray1335[i] & 0xff;
	}

	@Override
	public void requestModel(int id) {
		request(0, id);
	}

	public void method326(int i) {
		if (i != 0)
			return;
		while (anInt1342 == 0 && anInt1343 < 10) {
			if (anInt1341 == 0)
				break;
			FileNode class50_sub1_sub3;
			synchronized (aClass6_1358) {
				class50_sub1_sub3 = (FileNode) aClass6_1358.removeFirst();
			}
			while (class50_sub1_sub3 != null) {
				if (aByteArrayArray1337[class50_sub1_sub3.type][class50_sub1_sub3.id] != 0) {
					aByteArrayArray1337[class50_sub1_sub3.type][class50_sub1_sub3.id] = 0;
					aClass6_1374.addLast(class50_sub1_sub3);
					method342(anInt1345, class50_sub1_sub3);
					aBoolean1338 = true;
					if (anInt1334 < anInt1350)
						anInt1334++;
					aString1347 = "Loading extra files - " + (anInt1334 * 100) / anInt1350 + "%";
					anInt1343++;
					if (anInt1343 == 10)
						return;
				}
				synchronized (aClass6_1358) {
					class50_sub1_sub3 = (FileNode) aClass6_1358.removeFirst();
				}
			}
			for (int j = 0; j < 4; j++) {
				byte abyte0[] = aByteArrayArray1337[j];
				int k = abyte0.length;
				for (int l = 0; l < k; l++)
					if (abyte0[l] == anInt1341) {
						abyte0[l] = 0;
						FileNode class50_sub1_sub3_1 = new FileNode();
						class50_sub1_sub3_1.type = j;
						class50_sub1_sub3_1.id = l;
						class50_sub1_sub3_1.immediate = false;
						aClass6_1374.addLast(class50_sub1_sub3_1);
						method342(anInt1345, class50_sub1_sub3_1);
						aBoolean1338 = true;
						if (anInt1334 < anInt1350)
							anInt1334++;
						aString1347 = "Loading extra files - " + (anInt1334 * 100) / anInt1350 + "%";
						anInt1343++;
						if (anInt1343 == 10)
							return;
					}

			}

			anInt1341--;
		}
	}

	public void method327(int i, int j, byte byte0, int k) {
		while (i >= 0)
			return;
		if (_client.stores[0] == null)
			return;
		if (anIntArrayArray1377[j][k] == 0)
			return;
		byte abyte0[] = _client.stores[j + 1].get(k);
		if (method341(abyte0, 764, anIntArrayArray1377[j][k], anIntArrayArray1344[j][k]))
			return;
		aByteArrayArray1337[j][k] = byte0;
		if (byte0 > anInt1341)
			anInt1341 = byte0;
		anInt1350++;
	}

	public boolean method328(int i, boolean flag) {
		if (!flag)
			throw new NullPointerException();
		return anIntArray1366[i] == 1;
	}

	public void request(int type, int id) {
		if (type < 0 || type > anIntArrayArray1377.length || id < 0 || id > anIntArrayArray1377[type].length)
			return;
		if (anIntArrayArray1377[type][id] == 0)
			return;
		synchronized (aClass9_1369) {
			for (FileNode class50_sub1_sub3 = (FileNode) aClass9_1369.first(); class50_sub1_sub3 != null; class50_sub1_sub3 = (FileNode) aClass9_1369
					.next())
				if (class50_sub1_sub3.type == type && class50_sub1_sub3.id == id)
					return;

			FileNode class50_sub1_sub3_1 = new FileNode();
			class50_sub1_sub3_1.type = type;
			class50_sub1_sub3_1.id = id;
			class50_sub1_sub3_1.immediate = true;
			synchronized (aClass6_1340) {
				aClass6_1340.addLast(class50_sub1_sub3_1);
			}
			aClass9_1369.push(class50_sub1_sub3_1);
		}
	}

	public FileNode method330() {
		FileNode class50_sub1_sub3;
		synchronized (completed) {
			class50_sub1_sub3 = (FileNode) completed.removeFirst();
		}
		if (class50_sub1_sub3 == null)
			return null;
		synchronized (aClass9_1369) {
			class50_sub1_sub3.unlinkFromQueue();
		}
		if (class50_sub1_sub3.buf == null)
			return class50_sub1_sub3;
		int i = 0;
		try {
			GZIPInputStream gzipinputstream = new GZIPInputStream(new ByteArrayInputStream(class50_sub1_sub3.buf));
			do {
				if (i == aByteArray1359.length)
					throw new RuntimeException("buffer overflow!");
				int k = gzipinputstream.read(aByteArray1359, i, aByteArray1359.length - i);
				if (k == -1)
					break;
				i += k;
			} while (true);
		} catch (IOException _ex) {
			throw new RuntimeException("error unzipping");
		}
		class50_sub1_sub3.buf = new byte[i];
		for (int j = 0; j < i; j++)
			class50_sub1_sub3.buf[j] = aByteArray1359[j];

		return class50_sub1_sub3;
	}

	public void run() {
		try {
			while (aBoolean1339) {
				anInt1348++;
				int delay = 20;
				if (anInt1341 == 0 && _client.stores[0] != null)
					delay = 50;
				try {
					Thread.sleep(delay);
				} catch (Exception _ex) {
				}
				aBoolean1338 = true;
				for (int j = 0; j < 100; j++) {
					if (!aBoolean1338)
						break;
					aBoolean1338 = false;
					method338(true);
					method331(0);
					if (anInt1342 == 0 && j >= 5)
						break;
					method326(0);
					if (in != null)
						waitForResponse();
				}

				boolean flag = false;
				for (FileNode class50_sub1_sub3 = (FileNode) aClass6_1374.first(); class50_sub1_sub3 != null; class50_sub1_sub3 = (FileNode) aClass6_1374.next())
					if (class50_sub1_sub3.immediate) {
						flag = true;
						class50_sub1_sub3.cycles++;
						if (class50_sub1_sub3.cycles > 50) {
							class50_sub1_sub3.cycles = 0;
							method342(anInt1345, class50_sub1_sub3);
						}
					}

				if (!flag) {
					for (FileNode class50_sub1_sub3_1 = (FileNode) aClass6_1374.first(); class50_sub1_sub3_1 != null; class50_sub1_sub3_1 = (FileNode) aClass6_1374.next()) {
						flag = true;
						class50_sub1_sub3_1.cycles++;
						if (class50_sub1_sub3_1.cycles > 50) {
							class50_sub1_sub3_1.cycles = 0;
							method342(anInt1345, class50_sub1_sub3_1);
						}
					}

				}
				if (flag) {
					anInt1353++;
					if (anInt1353 > 750) {
						try {
							aSocket1355.close();
						} catch (Exception _ex) {
						}
						aSocket1355 = null;
						in = null;
						anOutputStream1349 = null;
						remaining = 0;
					}
				} else {
					anInt1353 = 0;
					aString1347 = "";
				}
				if (_client.aBoolean1137 && aSocket1355 != null && anOutputStream1349 != null
						&& (anInt1341 > 0 || _client.stores[0] == null)) {
					anInt1375++;
					if (anInt1375 > 500) {
						anInt1375 = 0;
						tmpBuf[0] = 0;
						tmpBuf[1] = 0;
						tmpBuf[2] = 0;
						tmpBuf[3] = 10;
						try {
							anOutputStream1349.write(tmpBuf, 0, 4);
						} catch (IOException _ex) {
							anInt1353 = 5000;
						}
					}
				}
			}
			return;
		} catch (Exception exception) {
			signlink.reporterror("od_ex " + exception.getMessage());
		}
	}

	public void method331(int i) {
		anInt1342 = 0;
		anInt1343 = 0;
		if (i != 0)
			return;
		for (FileNode class50_sub1_sub3 = (FileNode) aClass6_1374.first(); class50_sub1_sub3 != null; class50_sub1_sub3 = (FileNode) aClass6_1374
				.next())
			if (class50_sub1_sub3.immediate)
				anInt1342++;
			else
				anInt1343++;

		while (anInt1342 < 10) {
			FileNode class50_sub1_sub3_1 = (FileNode) aClass6_1351.removeFirst();
			if (class50_sub1_sub3_1 == null)
				break;
			if (aByteArrayArray1337[class50_sub1_sub3_1.type][class50_sub1_sub3_1.id] != 0)
				anInt1334++;
			aByteArrayArray1337[class50_sub1_sub3_1.type][class50_sub1_sub3_1.id] = 0;
			aClass6_1374.addLast(class50_sub1_sub3_1);
			anInt1342++;
			method342(anInt1345, class50_sub1_sub3_1);
			aBoolean1338 = true;
		}
	}

	public void method332(boolean flag, byte byte0) {
		if (byte0 != 109)
			aBoolean1352 = !aBoolean1352;
		int i = anIntArray1346.length;
		for (int j = 0; j < i; j++)
			if (flag || anIntArray1336[j] != 0) {
				method327(-44, 3, (byte) 2, anIntArray1365[j]);
				method327(-44, 3, (byte) 2, anIntArray1360[j]);
			}

	}

	public int method333() {
		synchronized (aClass9_1369) {
			int i = aClass9_1369.size();
			return i;
		}
	}

	public boolean method334(int i, boolean flag) {
		for (int j = 0; j < anIntArray1346.length; j++)
			if (anIntArray1365[j] == i)
				return true;

		if (flag)
			anInt1363 = -405;
		return false;
	}

	public void init(Archive archive, client client1) {
		String as[] = { "model_version", "anim_version", "midi_version", "map_version" };
		for (int i = 0; i < 4; i++) {
			byte abyte0[] = archive.get(as[i]);
			int j = abyte0.length / 2;
			JagBuffer class50_sub1_sub2 = new JagBuffer(abyte0);
			anIntArrayArray1377[i] = new int[j];
			aByteArrayArray1337[i] = new byte[j];
			for (int l = 0; l < j; l++)
				anIntArrayArray1377[i][l] = class50_sub1_sub2.getShort();

		}

		String as1[] = { "model_crc", "anim_crc", "midi_crc", "map_crc" };
		for (int k = 0; k < 4; k++) {
			byte abyte1[] = archive.get(as1[k]);
			int i1 = abyte1.length / 4;
			JagBuffer class50_sub1_sub2_1 = new JagBuffer(abyte1);
			anIntArrayArray1344[k] = new int[i1];
			for (int l1 = 0; l1 < i1; l1++)
				anIntArrayArray1344[k][l1] = class50_sub1_sub2_1.getInt();

		}

		byte abyte2[] = archive.get("model_index");
		int j1 = anIntArrayArray1377[0].length;
		aByteArray1335 = new byte[j1];
		for (int k1 = 0; k1 < j1; k1++)
			if (k1 < abyte2.length)
				aByteArray1335[k1] = abyte2[k1];
			else
				aByteArray1335[k1] = 0;

		abyte2 = archive.get("map_index");
		JagBuffer class50_sub1_sub2_2 = new JagBuffer(abyte2);
		j1 = abyte2.length / 7;
		anIntArray1346 = new int[j1];
		anIntArray1360 = new int[j1];
		anIntArray1365 = new int[j1];
		anIntArray1336 = new int[j1];
		for (int i2 = 0; i2 < j1; i2++) {
			anIntArray1346[i2] = class50_sub1_sub2_2.getShort();
			anIntArray1360[i2] = class50_sub1_sub2_2.getShort();
			anIntArray1365[i2] = class50_sub1_sub2_2.getShort();
			anIntArray1336[i2] = class50_sub1_sub2_2.getByte();
		}

		abyte2 = archive.get("anim_index");
		class50_sub1_sub2_2 = new JagBuffer(abyte2);
		j1 = abyte2.length / 2;
		anIntArray1376 = new int[j1];
		for (int j2 = 0; j2 < j1; j2++)
			anIntArray1376[j2] = class50_sub1_sub2_2.getShort();

		abyte2 = archive.get("midi_index");
		class50_sub1_sub2_2 = new JagBuffer(abyte2);
		j1 = abyte2.length;
		anIntArray1366 = new int[j1];
		for (int k2 = 0; k2 < j1; k2++)
			anIntArray1366[k2] = class50_sub1_sub2_2.getByte();

		_client = client1;
		aBoolean1339 = true;
		_client.startThread(this, 2);
	}

	public void method336(byte byte0) {
		synchronized (aClass6_1358) {
			aClass6_1358.clear();
		}
		if (byte0 != -125)
			aBoolean1352 = !aBoolean1352;
	}

	public void method337(int i, int j, byte byte0) {
		if (_client.stores[0] == null)
			return;
		if (anIntArrayArray1377[j][i] == 0)
			return;
		if (aByteArrayArray1337[j][i] == 0)
			return;
		if (anInt1341 == 0)
			return;
		FileNode class50_sub1_sub3 = new FileNode();
		if (byte0 != -113)
			anInt1367 = 244;
		class50_sub1_sub3.type = j;
		class50_sub1_sub3.id = i;
		class50_sub1_sub3.immediate = false;
		synchronized (aClass6_1358) {
			aClass6_1358.addLast(class50_sub1_sub3);
		}
	}

	public void method338(boolean flag) {
		FileNode class50_sub1_sub3;
		synchronized (aClass6_1340) {
			class50_sub1_sub3 = (FileNode) aClass6_1340.removeFirst();
		}
		if (!flag) {
			for (int i = 1; i > 0; i++);
		}
		while (class50_sub1_sub3 != null) {
			aBoolean1338 = true;
			byte abyte0[] = null;
			if (_client.stores[0] != null)
				abyte0 = _client.stores[class50_sub1_sub3.type + 1].get(class50_sub1_sub3.id);
			if (!method341(abyte0, 764, anIntArrayArray1377[class50_sub1_sub3.type][class50_sub1_sub3.id],
					anIntArrayArray1344[class50_sub1_sub3.type][class50_sub1_sub3.id]))
				abyte0 = null;
			synchronized (aClass6_1340) {
				if (abyte0 == null) {
					aClass6_1351.addLast(class50_sub1_sub3);
				} else {
					class50_sub1_sub3.buf = abyte0;
					synchronized (completed) {
						completed.addLast(class50_sub1_sub3);
					}
				}
				class50_sub1_sub3 = (FileNode) aClass6_1340.removeFirst();
			}
		}
	}

	public void method339() {
		aBoolean1339 = false;
	}

	public int method340(int i, int j) {
		if (j != -31140)
			aBoolean1380 = !aBoolean1380;
		return anIntArrayArray1377[i].length;
	}

	public boolean method341(byte abyte0[], int i, int j, int k) {
		i = 22 / i;
		if (abyte0 == null || abyte0.length < 2)
			return false;
		int l = abyte0.length - 2;
		int i1 = ((abyte0[l] & 0xff) << 8) + (abyte0[l + 1] & 0xff);
		aCRC32_1354.reset();
		aCRC32_1354.update(abyte0, 0, l);
		int j1 = (int) aCRC32_1354.getValue();
		if (i1 != j)
			return false;
		return j1 == k;
	}

	public void method342(int i, FileNode file) {
		if (i != 0)
			return;
		try {
			if (aSocket1355 == null) {
				long l = System.currentTimeMillis();
				if (l - aLong1378 < 4000L)
					return;
				aLong1378 = l;
				aSocket1355 = _client.openSocket(43594 + client.portOffset);
				in = aSocket1355.getInputStream();
				anOutputStream1349 = aSocket1355.getOutputStream();
				anOutputStream1349.write(15);
				for (int j = 0; j < 8; j++)
					in.read();

				anInt1353 = 0;
			}
			tmpBuf[0] = (byte) file.type;
			tmpBuf[1] = (byte) (file.id >> 8);
			tmpBuf[2] = (byte) file.id;
			if (file.immediate)
				tmpBuf[3] = 2;
			else if (!_client.aBoolean1137)
				tmpBuf[3] = 1;
			else
				tmpBuf[3] = 0;
			anOutputStream1349.write(tmpBuf, 0, 4);
			anInt1375 = 0;
			anInt1379 = -10000;
			return;
		} catch (IOException ioexception) {
		}
		try {
			aSocket1355.close();
		} catch (Exception _ex) {
		}
		aSocket1355 = null;
		in = null;
		anOutputStream1349 = null;
		remaining = 0;
		anInt1379++;
	}

	public int method343(int i) {
		i = 0 / i;
		return anIntArray1376.length;
	}

	public int method344(int i, int j, int k, int l) {
		if (i != 0)
			return 1;
		int i1 = (j << 8) + k;
		for (int j1 = 0; j1 < anIntArray1346.length; j1++)
			if (anIntArray1346[j1] == i1)
				if (l == 0)
					return anIntArray1360[j1];
				else
					return anIntArray1365[j1];

		return -1;
	}

	public OnDemandFetcher() {
		aByteArrayArray1337 = new byte[4][];
		aBoolean1338 = false;
		aBoolean1339 = true;
		aClass6_1340 = new LinkedList();
		anIntArrayArray1344 = new int[4][];
		aString1347 = "";
		aClass6_1351 = new LinkedList();
		aBoolean1352 = false;
		aCRC32_1354 = new CRC32();
		aBoolean1356 = false;
		completed = new LinkedList();
		aClass6_1358 = new LinkedList();
		aByteArray1359 = new byte[65000];
		tmpBuf = new byte[500];
		anInt1367 = 591;
		aClass9_1369 = new LinkedQueue();
		aByte1371 = 6;
		aClass6_1374 = new LinkedList();
		anIntArrayArray1377 = new int[4][];
		aBoolean1380 = false;
	}

	public int anInt1334;
	public byte aByteArray1335[];
	public int anIntArray1336[];
	public byte aByteArrayArray1337[][];
	public boolean aBoolean1338;
	public boolean aBoolean1339;
	public LinkedList aClass6_1340;
	public int anInt1341;
	public int anInt1342;
	public int anInt1343;
	public int anIntArrayArray1344[][];
	public int anInt1345;
	public int anIntArray1346[];
	public String aString1347;
	public int anInt1348;
	public OutputStream anOutputStream1349;
	public int anInt1350;
	public LinkedList aClass6_1351;
	public boolean aBoolean1352;
	public int anInt1353;
	public CRC32 aCRC32_1354;
	public Socket aSocket1355;
	public boolean aBoolean1356;
	public LinkedList completed;
	public LinkedList aClass6_1358;
	public byte aByteArray1359[];
	public int anIntArray1360[];
	public int offset;
	public int remaining;
	public int anInt1363;
	public byte tmpBuf[];
	public int anIntArray1365[];
	public int anIntArray1366[];
	public int anInt1367;
	public int anInt1368;
	public LinkedQueue aClass9_1369;
	public InputStream in;
	public byte aByte1371;
	public FileNode currentFile;
	public client _client;
	public LinkedList aClass6_1374;
	public int anInt1375;
	public int anIntArray1376[];
	public int anIntArrayArray1377[][];
	public long aLong1378;
	public int anInt1379;
	public boolean aBoolean1380;
}
