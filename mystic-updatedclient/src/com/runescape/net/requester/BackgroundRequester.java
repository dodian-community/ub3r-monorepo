package com.runescape.net.requester;

import com.runescape.Client;

/**
 * Requests to download cache files in the background
 * from the File-Server.
 * @author Professor Oak
 */
public class BackgroundRequester implements Runnable {

	@Override
	public void run() {
		try {
			
			//The file type
			for(int type = 0; type < 4; type++) {
				
				//The file
				for(int file = 0; file < getAmountToDownload(type); file++) {
					
					//Request the file
					if(Client.instance.resourceProvider != null) {
						Client.instance.resourceProvider.provide(type, file);
					}
					
					//Sleep
					Thread.sleep(120);
				}
			}
		
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private int getAmountToDownload(int type) {
		switch(type) {
		case 0: //Models
			return 32807;
		case 1: //anims
			return 1894;
		case 2: //Sounds
			return 0; //Dont download any sounds
		case 3: //Maps
			return 3535;
		}
		return 0;
	}
}
