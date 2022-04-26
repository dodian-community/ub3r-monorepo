package net.dodian.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;


public class Jframe extends Client implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JFrame frame;

    public Jframe(String args[]) {
        super();
        try {
            Signlink.startpriv(InetAddress.getByName(server));
            initUI();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            frame = new JFrame("Dodian Version 1.1");
            frame.setLayout(new BorderLayout());
            setFocusTraversalKeysEnabled(false);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JPanel gamePanel = new JPanel();

            gamePanel.setLayout(new BorderLayout());
            gamePanel.add(this);
            gamePanel.setPreferredSize(new Dimension(765, 503));

            JMenu fileMenu = new JMenu("File");

            String[] mainButtons = new String[]{"Rune-Server", "-", "Exit"};

            for (String name : mainButtons) {
                JMenuItem menuItem = new JMenuItem(name);
                if (name.equalsIgnoreCase("-")) {
                    fileMenu.addSeparator();
                } else {
                    menuItem.addActionListener(this);
                    fileMenu.add(menuItem);
                }
            }

            JMenuBar menuBar = new JMenuBar();
            JMenuBar jmenubar = new JMenuBar();

            frame.add(jmenubar);
            menuBar.add(fileMenu);
            frame.getContentPane().add(gamePanel, BorderLayout.CENTER);
            frame.pack();

            frame.setVisible(true); // can see the client
            frame.setResizable(false); // resizeable frame

            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public URL getCodeBase() {
        try {
            return new URL("http://" + server + "/cache");
        } catch (Exception e) {
            return super.getCodeBase();
        }
    }

    public URL getDocumentBase() {
        return getCodeBase();
    }

    public void loadError(String s) {
        System.out.println("loadError: " + s);
    }

    public String getParameter(String key) {
        return "";
    }

    private static void openUpWebSite(String url) {
        Desktop d = Desktop.getDesktop();
        try {
            d.browse(new URI(url));
        } catch (Exception e) {
        }
    }

    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();
        try {
            if (cmd != null) {
                if (cmd.equalsIgnoreCase("exit")) {
                    System.exit(0);
                }
                if (cmd.equalsIgnoreCase("Rune-Server")) {
                    openUpWebSite("http://www.rune-server.org/");
                }
            }
        } catch (Exception e) {
        }
    }
}