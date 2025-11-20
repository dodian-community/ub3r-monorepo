package com.runescape.io.jaggrab;

import java.nio.charset.Charset;

public class JagGrabConstants {

	//Files that will always be updated & downloaded from the update-server
	//NOTE: These exact files must be defined in the update-server's PRELOAD_FILES
	//array aswell to prevent CRC-checking issues.
	public static final String[] PRELOAD_FILES = {
			"sprites.idx", "sprites.dat", 
			"obj.idx", "obj.dat"
	};
	
	/**
	 * The port for the file-server service.
	 */
	public static final int FILE_SERVER_PORT = 43596;
	
	/**
	 * The opcode for a JagGrab request.
	 */
	public static final byte JAGGRAB_REQUEST_OPCODE = 1;
	
	/**
	 * The opcode for an OnDemand request.
	 */
	public static final byte ONDEMAND_REQUEST_OPCODE = 2;	
	
	/**
	 * The character set used in a JagGrab request.
	 */
	public static final Charset JAGGRAB_CHARSET = Charset.forName("US-ASCII");
	
	/**
	 * The maximum length of an ondemand file chunk, in bytes.
	 */
	public static final int MAX_ONDEMAND_CHUNK_LENGTH_BYTES = 500;
}
