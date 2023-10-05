package com.jagex.runescape;// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    public final GameShell shell;

    public GameFrame(GameShell shell) {
        this.shell = shell;
        setTitle("Uber Server 3.0 - Client Version 1.0.0");
        setResizable(false);
        setLayout(new BorderLayout());
        add(shell, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        transferFocus();
    }

}
