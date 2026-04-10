// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import javax.swing.*;
import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

@SuppressWarnings("serial")
public class JagApplet extends Applet implements Runnable, MouseListener, MouseMotionListener, KeyListener,
		FocusListener, WindowListener {

	public void start(int _width, int _height) {
		width = _width;
		height = _height;
		frame = new JagFrame(width, height, this);
		graphics = getParentComponent().getGraphics();
		imageProducer = new JagImageProducer(width, height, getParentComponent());
		startThread(this, 1);
	}

	public void method2(int i, int j, int k) {
		width = i;
		if (k != 2) {
			return;
		} else {
			height = j;
			graphics = getParentComponent().getGraphics();
			imageProducer = new JagImageProducer(width, height, getParentComponent());
			startThread(this, 1);
			return;
		}
	}

	public void run() {
		getParentComponent().addMouseListener(this);
		getParentComponent().addMouseMotionListener(this);
		getParentComponent().addKeyListener(this);
		getParentComponent().addFocusListener(this);
		if (frame != null)
			frame.addWindowListener(this);
		drawLoadingText(0, "Loading...");
		method6();
		int i = 0;
		int j = 256;
		int k = 1;
		int i1 = 0;
		int j1 = 0;
		for (int k1 = 0; k1 < 10; k1++)
			aLongArray9[k1] = System.currentTimeMillis();

		while (gameState >= 0) {
			if (gameState > 0) {
				gameState--;
				if (gameState == 0) {
					exit(aBoolean2);
					return;
				}
			}
			int i2 = j;
			int j2 = k;
			j = 300;
			k = 1;
			long l1 = System.currentTimeMillis();
			if (aLongArray9[i] == 0L) {
				j = i2;
				k = j2;
			} else if (l1 > aLongArray9[i])
				j = (int) ((2560 * anInt7) / (l1 - aLongArray9[i]));
			if (j < 25)
				j = 25;
			if (j > 256) {
				j = 256;
				k = (int) (anInt7 - (l1 - aLongArray9[i]) / 10L);
			}
			if (k > anInt7)
				k = anInt7;
			aLongArray9[i] = l1;
			i = (i + 1) % 10;
			if (k > 1) {
				for (int k2 = 0; k2 < 10; k2++)
					if (aLongArray9[k2] != 0L)
						aLongArray9[k2] += k;

			}
			if (k < anInt8)
				k = anInt8;
			try {
				Thread.sleep(k);
			} catch (InterruptedException _ex) {
				j1++;
			}
			for (; i1 < 256; i1 += j) {
				anInt28 = anInt24;
				anInt29 = anInt25;
				anInt30 = anInt26;
				aLong31 = aLong27;
				anInt24 = 0;
				method7((byte) -111);
				anInt34 = anInt35;
			}

			i1 &= 0xff;
			if (anInt7 > 0)
				fps = (1000 * j) / (anInt7 * 256);
			repaintGame(818);
			if (aBoolean11) {
				System.out.println("ntime:" + l1);
				for (int l2 = 0; l2 < 10; l2++) {
					int i3 = ((i - l2 - 1) + 20) % 10;
					System.out.println("otim" + i3 + ":" + aLongArray9[i3]);
				}

				System.out.println("fps:" + fps + " ratio:" + j + " count:" + i1);
				System.out.println("del:" + k + " deltime:" + anInt7 + " mindel:" + anInt8);
				System.out.println("intex:" + j1 + " opos:" + i);
				aBoolean11 = false;
				j1 = 0;
			}
		}
		if (gameState == -1)
			exit(aBoolean2);
	}

	public void exit(boolean flag) {
		gameState = -2;
		method8(277);
		if (flag)
			return;
		if (frame != null) {
			try {
				Thread.sleep(1000L);
			} catch (Exception _ex) {
			}
			try {
				System.exit(0);
				return;
			} catch (Throwable _ex) {
			}
		}
	}

	public void method4(int i) {
		anInt7 = 1000 / i;
	}

	@Override
	public void start() {
		if (gameState >= 0)
			gameState = 0;
	}

	@Override
	public void stop() {
		if (gameState >= 0)
			gameState = 4000 / anInt7;
	}

	@Override
	public void destroy() {
		gameState = -1;
		try {
			Thread.sleep(10000L);
		} catch (Exception _ex) {
		}
		if (gameState == -1)
			exit(aBoolean2);
	}

	@Override
	public void update(Graphics g) {
		if (graphics == null)
			graphics = g;
		clearBackground = true;
		method10((byte) -99);
	}

	@Override
	public void paint(Graphics g) {
		if (graphics == null)
			graphics = g;
		clearBackground = true;
		method10((byte) -99);
	}

	public void mousePressed(MouseEvent mouseevent) {
		int i = mouseevent.getX();
		int j = mouseevent.getY();
		if (frame != null) {
			i -= 4;
			j -= 22;
		}
		anInt20 = 0;
		anInt25 = i;
		anInt26 = j;
		aLong27 = System.currentTimeMillis();
		if (SwingUtilities.isRightMouseButton(mouseevent)) {
			anInt24 = 2;
			anInt21 = 2;
			return;
		} else if (SwingUtilities.isLeftMouseButton(mouseevent)){
			anInt24 = 1;
			anInt21 = 1;
			return;
		}
	}

	public void mouseReleased(MouseEvent mouseevent) {
		anInt20 = 0;
		anInt21 = 0;
	}

	public void mouseClicked(MouseEvent mouseevent) {
	}

	public void mouseEntered(MouseEvent mouseevent) {
	}

	public void mouseExited(MouseEvent mouseevent) {
		anInt20 = 0;
		mouseX = -1;
		mouseY = -1;
	}

	public void mouseDragged(MouseEvent mouseevent) {
		int i = mouseevent.getX();
		int j = mouseevent.getY();
		if (frame != null) {
			i -= 4;
			j -= 22;
		}
		anInt20 = 0;
		mouseX = i;
		mouseY = j;
	}

	public void mouseMoved(MouseEvent mouseevent) {
		int i = mouseevent.getX();
		int j = mouseevent.getY();
		if (frame != null) {
			i -= 4;
			j -= 22;
		}
		anInt20 = 0;
		mouseX = i;
		mouseY = j;
	}

	public void keyPressed(KeyEvent keyevent) {
		anInt20 = 0;
		int i = keyevent.getKeyCode();
		int j = keyevent.getKeyChar();
		if (j < 30)
			j = 0;
		if (i == 37)
			j = 1;
		if (i == 39)
			j = 2;
		if (i == 38)
			j = 3;
		if (i == 40)
			j = 4;
		if (i == 17)
			j = 5;
		if (i == 8)
			j = 8;
		if (i == 127)
			j = 8;
		if (i == 9)
			j = 9;
		if (i == 10)
			j = 10;
		if (i >= 112 && i <= 123)
			j = (1008 + i) - 112;
		if (i == 36)
			j = 1000;
		if (i == 35)
			j = 1001;
		if (i == 33)
			j = 1002;
		if (i == 34)
			j = 1003;
		if (j > 0 && j < 128)
			anIntArray32[j] = 1;
		if (j > 4) {
			anIntArray33[anInt35] = j;
			anInt35 = anInt35 + 1 & 0x7f;
		}
	}

	public void keyReleased(KeyEvent keyevent) {
		anInt20 = 0;
		int i = keyevent.getKeyCode();
		char c = keyevent.getKeyChar();
		if (c < '\036')
			c = '\0';
		if (i == 37)
			c = '\001';
		if (i == 39)
			c = '\002';
		if (i == 38)
			c = '\003';
		if (i == 40)
			c = '\004';
		if (i == 17)
			c = '\005';
		if (i == 8)
			c = '\b';
		if (i == 127)
			c = '\b';
		if (i == 9)
			c = '\t';
		if (i == 10)
			c = '\n';
		if (c > 0 && c < '\200')
			anIntArray32[c] = 0;
	}

	public void keyTyped(KeyEvent keyevent) {
	}

	public int method5(int i) {
		while (i >= 0)
			anInt5 = -9;
		int j = -1;
		if (anInt35 != anInt34) {
			j = anIntArray33[anInt34];
			anInt34 = anInt34 + 1 & 0x7f;
		}
		return j;
	}

	public void focusGained(FocusEvent focusevent) {
		aBoolean19 = true;
		clearBackground = true;
		method10((byte) -99);
	}

	public void focusLost(FocusEvent focusevent) {
		aBoolean19 = false;
		for (int i = 0; i < 128; i++)
			anIntArray32[i] = 0;

	}

	public void windowActivated(WindowEvent windowevent) {
	}

	public void windowClosed(WindowEvent windowevent) {
	}

	public void windowClosing(WindowEvent windowevent) {
		destroy();
	}

	public void windowDeactivated(WindowEvent windowevent) {
	}

	public void windowDeiconified(WindowEvent windowevent) {
	}

	public void windowIconified(WindowEvent windowevent) {
	}

	public void windowOpened(WindowEvent windowevent) {
	}

	public void method6() {
	}

	public void method7(byte byte0) {
		if (byte0 != -111)
			anInt5 = -400;
	}

	public void method8(int i) {
		i = 41 / i;
	}

	public void repaintGame(int i) {
		if (i > 0)
			;
	}

	public void method10(byte byte0) {
		if (byte0 == -99)
			;
	}

	public Component getParentComponent() {
		if (frame != null)
			return frame;
		else
			return this;
	}

	public void startThread(Runnable runnable, int priority) {
		Thread thread = new Thread(runnable);
		thread.start();
		thread.setPriority(priority);
	}

	public void drawLoadingText(int percent, String desc) {
		while (graphics == null) {
			graphics = getParentComponent().getGraphics();
			try {
				getParentComponent().repaint();
			} catch (Exception _ex) {
			}
			try {
				Thread.sleep(1000L);
			} catch (Exception _ex) {
			}
		}
		Font font = new Font("Helvetica", 1, 13);
		FontMetrics fontmetrics = getParentComponent().getFontMetrics(font);
		Font font1 = new Font("Helvetica", 0, 13);
		getParentComponent().getFontMetrics(font1);
		if (clearBackground) {
			graphics.setColor(Color.black);
			graphics.fillRect(0, 0, width, height);
			clearBackground = false;
		}
		Color color = new Color(140, 17, 17);
		int j = height / 2 - 18;
		graphics.setColor(color);
		graphics.drawRect(width / 2 - 152, j, 304, 34);
		graphics.fillRect(width / 2 - 150, j + 2, percent * 3, 30);
		graphics.setColor(Color.black);
		graphics.fillRect((width / 2 - 150) + percent * 3, j + 2, 300 - percent * 3, 30);
		graphics.setFont(font);
		graphics.setColor(Color.white);
		graphics.drawString(desc, (width - fontmetrics.stringWidth(desc)) / 2, j + 22);
	}

	public JagApplet() {
		aBoolean2 = false;
		aBoolean3 = false;
		anInt4 = 3;
		anInt7 = 20;
		anInt8 = 1;
		aLongArray9 = new long[10];
		aBoolean11 = false;
		aClass50_Sub1_Sub1_Sub1Array16 = new RgbSprite[6];
		clearBackground = true;
		aBoolean19 = true;
		anIntArray32 = new int[128];
		anIntArray33 = new int[128];
	}

	public boolean aBoolean2;
	public boolean aBoolean3;
	public int anInt4;
	public int anInt5;
	public int gameState;
	public int anInt7;
	public int anInt8;
	public long aLongArray9[];
	public int fps;
	public boolean aBoolean11;
	public int width;
	public int height;
	public Graphics graphics;
	public JagImageProducer imageProducer;
	public RgbSprite aClass50_Sub1_Sub1_Sub1Array16[];
	public JagFrame frame;
	public boolean clearBackground;
	public boolean aBoolean19;
	public int anInt20;
	public int anInt21;
	public int mouseX;
	public int mouseY;
	public int anInt24;
	public int anInt25;
	public int anInt26;
	public long aLong27;
	public int anInt28;
	public int anInt29;
	public int anInt30;
	public long aLong31;
	public int anIntArray32[];
	public int anIntArray33[];
	public int anInt34;
	public int anInt35;
	public static int anInt36;
}
