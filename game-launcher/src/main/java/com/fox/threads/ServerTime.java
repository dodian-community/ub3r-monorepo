package com.fox.threads;

import com.fox.components.AppFrame;
import com.fox.utils.Utils;

public class ServerTime implements Runnable {
	
	@Override
	public void run() {
		
		while (true) {
				AppFrame.serverTime.setText("Server Time: "+Utils.getServerTime());
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
	}

}
