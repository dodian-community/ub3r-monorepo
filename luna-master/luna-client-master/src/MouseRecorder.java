// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class MouseRecorder implements Runnable {

	public MouseRecorder(client _client) {
		running = true;
		mouseY = new int[500];
		lock = new Object();
		mouseX = new int[500];
		client = _client;
	}

	public void run() {
		while (running) {
			synchronized (lock) {
				if (pos < 500) {
					mouseX[pos] = ((JagApplet) (client)).mouseX;
					mouseY[pos] = ((JagApplet) (client)).mouseY;
					pos++;
				}
			}
			try {
				Thread.sleep(50L);
			} catch (Exception _ex) {
			}
		}
	}

	public boolean running;
	public int mouseY[];
	public Object lock;
	public client client;
	public int pos;
	public int mouseX[];
}
