package net.dodian.client;

import java.awt.*;

final class RSFrame extends Frame {

    private static final long serialVersionUID = 1L;
    private String title = "Dodian.net net.dodian.client.Client - Uber Server 3.0";

    public RSFrame(RSApplet RSApplet_, int i, int j) {
        rsApplet = RSApplet_;
        setTitle(title);
        setVisible(true);
        toFront();
        setSize(i + 8, j + 28);
        setResizable(Client.clientSize == 0 ? false : true);
        setLocationRelativeTo(null);
    }

    public RSFrame(RSApplet rsapplet, int width, int height, boolean undecorative, boolean resizable) {
        rsApplet = rsapplet;
        setTitle(title);
        setUndecorated(undecorative);
        setBackground(Color.BLACK);
        setVisible(true);
        requestFocus();
        toFront();
        setResizable(Client.clientSize == 0 ? false : true);
        setFocusTraversalKeysEnabled(false);
        toFront();
        Insets insets = getInsets();
        setSize(width + insets.left + insets.right, height + insets.top + insets.bottom);
        setLocationRelativeTo(null);
    }

    public Graphics getGraphics() {
        Graphics g = super.getGraphics();
        Insets insets = this.getInsets();
        g.translate(insets.left, insets.top);
        return g;
    }

    public int getFrameWidth() {
        Insets insets = this.getInsets();
        return getWidth() - (insets.left + insets.right);
    }

    public int getFrameHeight() {
        Insets insets = this.getInsets();
        return getHeight() - (insets.top + insets.bottom);
    }

    public void update(Graphics g) {
        rsApplet.update(g);
    }

    public void paint(Graphics g) {
        rsApplet.paint(g);
    }

    private final RSApplet rsApplet;
    public Toolkit toolkit = Toolkit.getDefaultToolkit();
    public Dimension screenSize = toolkit.getScreenSize();
    public int screenWidth = (int) screenSize.getWidth();
    public int screenHeight = (int) screenSize.getHeight();
}
