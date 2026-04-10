// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.awt.Frame;
import java.awt.Graphics;

@SuppressWarnings("serial")
public class JagFrame extends Frame {

	public JagFrame(int width, int height, JagApplet _applet) {
		applet = _applet;
		setTitle("Jagex");
		setResizable(false);
		setSize(width + 8, height + 28);
		setVisible(true);
		toFront();
		return;
	}

	@Override
	public Graphics getGraphics() {
		Graphics g = super.getGraphics();
		g.translate(4, 24);
		return g;
	}

	@Override
	public void update(Graphics g) {
		applet.update(g);
	}

	@Override
	public void paint(Graphics g) {
		applet.paint(g);
	}

	public JagApplet applet;
}
