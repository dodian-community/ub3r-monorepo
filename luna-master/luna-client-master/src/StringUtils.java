// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.net.InetAddress;

public class StringUtils {

	public static long encodeBase37(String str) {
		long base37 = 0L;
		for (int i = 0; i < str.length() && i < 12; i++) {
			char ch = str.charAt(i);
			base37 *= 37L;
			if (ch >= 'A' && ch <= 'Z')
				base37 += (1 + ch) - 65;
			else if (ch >= 'a' && ch <= 'z')
				base37 += (1 + ch) - 97;
			else if (ch >= '0' && ch <= '9')
				base37 += (27 + ch) - 48;
		}

		for (; base37 % 37L == 0L && base37 != 0L; base37 /= 37L);
		return base37;
	}

	public static String decodeBase37(long base37) {
		if (base37 <= 0L || base37 >= 0x5b5b57f8a98a5dd1L)
			return "invalid_name";
		if (base37 % 37L == 0L)
			return "invalid_name";
		int len = 0;
		char ch[] = new char[12];
		while (base37 != 0L) {
			long tmp = base37;
			base37 /= 37L;
			ch[11 - len++] = BASE_37_CHARACTERS[(int) (tmp - base37 * 37L)];
		}
		return new String(ch, 12 - len, len);
	}

	public static long hash(String str) {
		str = str.toUpperCase();
		long hash = 0L;
		for (int pos = 0; pos < str.length(); pos++) {
			hash = (hash * 61L + str.charAt(pos)) - 32L;
			hash = hash + (hash >> 56) & 0xffffffffffffffL;
		}
		return hash;
	}

	public static String ipBitsToString(int ipBits) {
		return (ipBits >> 24 & 0xff) + "." + (ipBits >> 16 & 0xff) + "." + (ipBits >> 8 & 0xff) + "." + (ipBits & 0xff);
	}
	public static String formatPlayerName(String str) {
		if (str.length() > 0) {
			char ch[] = str.toCharArray();
			for (int pos = 0; pos < ch.length; pos++)
				if (ch[pos] == '_') {
					ch[pos] = ' ';
					if (pos + 1 < ch.length && ch[pos + 1] >= 'a' && ch[pos + 1] <= 'z')
						ch[pos + 1] = (char) ((ch[pos + 1] + 65) - 97);
				}

			if (ch[0] >= 'a' && ch[0] <= 'z')
				ch[0] = (char) ((ch[0] + 65) - 97);
			return new String(ch);
		} else {
			return str;
		}
	}

	public static String asterisks(String password) {
		StringBuffer buf = new StringBuffer();
		for (int k = 0; k < password.length(); k++)
			buf.append("*");
		return buf.toString();
	}

	public static final char BASE_37_CHARACTERS[] = { '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9' };

}
