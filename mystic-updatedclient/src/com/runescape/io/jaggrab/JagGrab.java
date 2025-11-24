package com.runescape.io.jaggrab;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.CRC32;

import org.seven.util.FileUtils;

import com.runescape.Client;
import com.runescape.io.Buffer;
import com.runescape.sign.SignLink;

/**
 * A class representing the Jaggrab
 * update-server system for a 317 client.
 * 
 * This system also supports preloadable
 * files, basically files which haven't
 * been packed into the main cache files.
 * 
 * @author Professor Oak
 */
public class JagGrab extends JagGrabConstants {

	//Archive CRCs
	public static final int TITLE_CRC = 1;
	public static final int CONFIG_CRC = 2;
	public static final int INTERFACE_CRC = 3;
	public static final int MEDIA_CRC = 4;
	public static final int UPDATE_CRC = 5;
	public static final int TEXTURES_CRC = 6;
	public static final int CHAT_CRC = 7;
	public static final int SOUNDS_CRC = 8;

	//CRCs
	public static final int TOTAL_ARCHIVE_CRCS = 9;
	public static final int[] CRCs = new int[TOTAL_ARCHIVE_CRCS + PRELOAD_FILES.length];

	/**
	 * CRC32 is one of hash functions based on on the "polynomial" division idea. The CRC is
	 * acronym for Cyclic Redundancy Code (other variants instead "Code" is "Check" and
	 * "Checksum") algorithm. The number 32 is specifying the size of resulting hash value
	 * (checksum The value calculated from content of file.
	 */
	public static CRC32 CRC = new CRC32();

	//Error attributes
	private static int failedAttempts;
	private static int delay = 5;
	
	
	public static Socket socket;

	/**
	 * This method is called upon client start.
	 * Loads all required files.
	 */
	public static void onStart() {
		//Download crc table
		requestCrcs();

		//Download preload files
		for(int i = 0; i < PRELOAD_FILES.length; i++) {
			requestPreload(i, PRELOAD_FILES[i]);
		}
	}


	/**
	 * Requests the crc table from the update-server
	 * and puts them into our array for future
	 * use.
	 * 
	 * Crcs are used for checking if files are
	 * up to date, and for other things aswell.
	 */
	public static void requestCrcs() {
		while(!crcsLoaded() && !Client.instance.exitRequested) {

			try (DataInputStream in = openJagGrabRequest("crc")) {

				Client.instance.drawLoadingText(20, "Requesting CRCs..");

				//Get incoming data
				Buffer buffer = new Buffer(getBuffer(in));

				for (int index = 0; index < CRCs.length; index++) {
					CRCs[index] = buffer.readInt();
				}

				int expected = buffer.readInt();
				int calculated = 1234;
				for (int index = 0; index < CRCs.length; index++) {
					calculated = (calculated << 1) + CRCs[index];
				}

				if (expected != calculated) {
					//Check sum error
					resetCrcs();
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				resetCrcs();
			}

			//Didn't load properly
			if (!crcsLoaded()) {
				error("CRC");
			}
		}
	}

	/**
	 * Preloads a file from the update-server
	 * if we dont have it or it's outdated (compares CRC).
	 * 
	 * Sends a request to the server and starts
	 * downloading the file.
	 * 
	 * @param i				The index of the preload file
	 * @param fileName		The name of the file to be preloaded
	 */
	private static void requestPreload(int i, String fileName) {
		boolean exists = false;
		byte[] buffer = null;

		//Check if file already exists...
		File file = new File(SignLink.findcachedir() + fileName);
		if(file.exists() && !file.isDirectory()) {
			exists = true;
			buffer = FileUtils.readFile(SignLink.findcachedir() + fileName);
		}

		//Check if the file is "updated" by comparing crc..
		if(buffer != null) {
			if (!compareCrc(buffer, CRCs[TOTAL_ARCHIVE_CRCS + i])) {
				buffer = null;
			}
		}

		//We already had the updated file!
		if(buffer != null) {
			return;
		}

		//Let's download the file.
		while(buffer == null && !Client.instance.exitRequested) {
			Client.instance.drawLoadingText(20, "Requesting " + fileName);
			
			try (DataInputStream in = openJagGrabRequest("preload" + "/" + fileName)) {

				//Try to get the file..
				buffer = getBuffer(in);

				//Compare crc again...
				if(buffer != null) {
					if (!compareCrc(buffer, CRCs[TOTAL_ARCHIVE_CRCS + i])) {
						buffer = null;
					}
				}

			} catch(Exception e) {
				e.printStackTrace();
			}

			if (buffer == null) {
				error("Preload");
			}
		}

		//Write the downloaded file..
		if(buffer != null && !exists) {
			try {
				FileUtils.writeFile(buffer, file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * An error occured during jaggrab load.
	 * Show the error on client.
	 * @param error		The error that occured.
	 */
	public static void error(String error) {
		failedAttempts++;
		for (int remaining = delay; remaining > 0; remaining--) {
			if (failedAttempts >= 10) {
				Client.instance.drawLoadingText(10, "Game updated - please reload page");
				remaining = 10;
			} else {
				Client.instance.drawLoadingText(10, ""+error+" Error - Retrying in " + remaining + " seconds.");
			}
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ex) {
			}
		}

		delay *= 2;
		if (delay > 60) {
			delay = 60;
		}
		

		failedAttempts = 0;
		delay = 2;
	}

	/**
	 * Gets the current buffer from the socket
	 * @param socket		The socket to read bytes from.
	 */
	public static byte[] getBuffer(DataInputStream in) throws IOException {
		
		//Read incoming file size..
		int size = in.readInt();
		if(size <= 0) {
			return null;
		}
				
		//Read incoming file..
		byte[] data = new byte[size];
		in.readFully(data, 0, size);
		/*	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[size];
		int read;
		while ((read = in.read(data)) != -1) {
		    buffer.write(data, 0, read);
		}
		
		return buffer.toByteArray();*/
		return data;
	}

	public static DataInputStream openJagGrabRequest(String filePath) throws IOException {

		//Close current socket
		if(socket != null) {
			try {
				socket.close();
			} catch(Exception e) {
			}
			socket = null;
		} 
		
		//Open new jaggrab request
		socket = Client.instance.openSocket(FILE_SERVER_PORT);
		socket.setSoTimeout(10000);
		InputStream inputstream = socket.getInputStream();
		OutputStream outputstream = socket.getOutputStream();
		
		//Write requested file to server
		byte[] filePathB = filePath.getBytes();
		
		byte[] payload = new byte[1 + filePathB.length];
		
		//Write opcode
		payload[0] = JagGrabConstants.JAGGRAB_REQUEST_OPCODE;
		
		//Write the string data
		for(int i = 0; i < filePathB.length; i++) {
			payload[i + 1] = filePathB[i];
		}
		
		outputstream.write(payload);
		
		//Create the DataInputStream
		return new DataInputStream(inputstream);
	}
	
	/**
	 * Compares crcs to check if the specified data is
	 * up to date or not.
	 * 
	 * @param buffer		The file data.
	 * @param expectedCrc	The expected result.
	 * @return
	 */
	public static boolean compareCrc(byte[] buffer, int expectedCrc) {
		CRC.reset();
		CRC.update(buffer);
		int crc = (int) CRC.getValue();		
		return crc == expectedCrc;
	}


	/**
	 * Checks if we have succesfully loaded all our crcs.
	 * @return
	 */
	private static boolean crcsLoaded() {
		for(int i = 0; i < CRCs.length; i++) {
			if(CRCs[i] == -1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Resets the crc table
	 */
	private static void resetCrcs() {
		for(int i = 0; i < CRCs.length; i++) {
			CRCs[i] = -1;
		}
	}

	static {
		resetCrcs();
	}
}
