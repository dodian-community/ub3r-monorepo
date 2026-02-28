package net.dodian.stress;

import net.dodian.stress.ui.StressTesterFrame;

import javax.swing.SwingUtilities;

public final class StressClientLauncher {

    private StressClientLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StressTesterFrame frame = new StressTesterFrame();
            frame.setVisible(true);
        });
    }
}
