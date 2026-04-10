// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class ChatCompressor {

	public static String decompress(JagBuffer buf, int len) {
		int pos = 0;
		int carry = -1;
		for (int i = 0; i < len; i++) {
			int b = buf.getByte();
			int nibble = b >> 4 & 0xf;
			if (carry == -1) {
				if (nibble < 13)
					chars[pos++] = FREQUENCY_ORDERED_CHARS[nibble];
				else
					carry = nibble;
			} else {
				chars[pos++] = FREQUENCY_ORDERED_CHARS[((carry << 4) + nibble) - 195];
				carry = -1;
			}
			nibble = b & 0xf;
			if (carry == -1) {
				if (nibble < 13)
					chars[pos++] = FREQUENCY_ORDERED_CHARS[nibble];
				else
					carry = nibble;
			} else {
				chars[pos++] = FREQUENCY_ORDERED_CHARS[((carry << 4) + nibble) - 195];
				carry = -1;
			}
		}

		boolean capitalize = true;
		for (int i = 0; i < pos; i++) {
			char c = chars[i];
			if (capitalize && c >= 'a' && c <= 'z') {
				chars[i] += '\uFFE0';
				capitalize = false;
			}
			if (c == '.' || c == '!' || c == '?')
				capitalize = true;
		}

		return new String(chars, 0, pos);
	}

	public static void compress(String str, JagBuffer buf) {
		if (str.length() > 80)
			str = str.substring(0, 80);
		str = str.toLowerCase();
		
		int carry = -1;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			int index = 0;
			for (int j = 0; j < FREQUENCY_ORDERED_CHARS.length; j++) {
				if (c != FREQUENCY_ORDERED_CHARS[j])
					continue;
				index = j;
				break;
			}

			if (index > 12)
				index += 195;
			if (carry == -1) {
				if (index < 13)
					carry = index;
				else
					buf.putByte(index);
			} else if (index < 13) {
				buf.putByte((carry << 4) + index);
				carry = -1;
			} else {
				buf.putByte((carry << 4) + (index >> 4));
				carry = index & 0xf;
			}
		}

		if (carry != -1)
			buf.putByte(carry << 4);
	}

	public static String format(String str) {
		buf.position = 0;
		compress(str, buf);
		int len = buf.position;
		buf.position = 0;
		return decompress(buf, len);
	}

	public static char chars[] = new char[100];
	public static JagBuffer buf = new JagBuffer(new byte[100]);
	public static char FREQUENCY_ORDERED_CHARS[] = { ' ', 'e', 't', 'a', 'o', 'i', 'h', 'n', 's', 'r', 'd', 'l', 'u', 'm', 'w',
			'c', 'y', 'f', 'g', 'p', 'b', 'v', 'k', 'x', 'j', 'q', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', ' ', '!', '?', '.', ',', ':', ';', '(', ')', '-', '&', '*', '\\', '\'', '@', '#', '+', '=', '\243',
			'$', '%', '"', '[', ']' };

}
