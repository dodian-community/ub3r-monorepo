package net.dodian.stress.ui;

import net.dodian.stress.StressStatsSnapshot;
import net.dodian.stress.StressTestConfig;
import net.dodian.stress.StressTestController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class StressTesterFrame extends JFrame {

    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("43594");
    private final JTextField prefixField = new JTextField("player");
    private final JTextField startField = new JTextField("1");
    private final JTextField botsField = new JTextField("100");
    private final JTextField rateField = new JTextField("10");
    private final JTextField passwordField = new JTextField("");
    private final JTextField keepAliveField = new JTextField("20");
    private final JTextField timeoutField = new JTextField("5000");

    private final JLabel runningValue = new JLabel("false");
    private final JLabel progressValue = new JLabel("0 / 0");
    private final JLabel successValue = new JLabel("0");
    private final JLabel failValue = new JLabel("0");
    private final JLabel activeValue = new JLabel("0");
    private final JLabel disconnectedValue = new JLabel("0");
    private final JLabel avgConnectValue = new JLabel("0 ms");
    private final JLabel p95ConnectValue = new JLabel("0 ms");
    private final JLabel maxConnectValue = new JLabel("0 ms");
    private final JLabel uptimeValue = new JLabel("00:00:00");

    private final JTextArea logArea = new JTextArea();
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");

    private final StressTestController controller = new StressTestController();
    private final Timer statsTimer = new Timer(500, e -> refreshStats());

    public StressTesterFrame() {
        super("RS317 Stress Client");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setLayout(new BorderLayout(8, 8));

        add(buildConfigPanel(), BorderLayout.NORTH);
        add(buildLogPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.stop();
            }
        });

        wireActions();
        statsTimer.start();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 6, 8, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Connection Setup"));

        panel.add(label("Host"));
        panel.add(label("Port"));
        panel.add(label("Username Prefix"));
        panel.add(label("Start Number"));
        panel.add(label("Bot Count"));
        panel.add(label("Connects / sec"));

        panel.add(hostField);
        panel.add(portField);
        panel.add(prefixField);
        panel.add(startField);
        panel.add(botsField);
        panel.add(rateField);

        panel.add(label("Password"));
        panel.add(label("KeepAlive (sec)"));
        panel.add(label("Connect Timeout (ms)"));
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));

        panel.add(passwordField);
        panel.add(keepAliveField);
        panel.add(timeoutField);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Session Log"));

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel status = new JPanel(new GridLayout(2, 5, 8, 4));
        status.setBorder(BorderFactory.createTitledBorder("Stats"));
        status.add(statusPanel("Running", runningValue));
        status.add(statusPanel("Progress", progressValue));
        status.add(statusPanel("Success", successValue));
        status.add(statusPanel("Failed", failValue));
        status.add(statusPanel("Active", activeValue));
        status.add(statusPanel("Disconnected", disconnectedValue));
        status.add(statusPanel("Avg Connect", avgConnectValue));
        status.add(statusPanel("P95 Connect", p95ConnectValue));
        status.add(statusPanel("Max Connect", maxConnectValue));
        status.add(statusPanel("Uptime", uptimeValue));

        JPanel actions = new JPanel();
        actions.add(startButton);
        actions.add(stopButton);
        stopButton.setEnabled(false);

        wrapper.add(status);
        wrapper.add(Box.createVerticalStrut(8));
        wrapper.add(actions);
        return wrapper;
    }

    private JPanel statusPanel(String key, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(key), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JLabel label(String value) {
        return new JLabel(value);
    }

    private void wireActions() {
        startButton.addActionListener(e -> onStart());
        stopButton.addActionListener(e -> onStop());
    }

    private void onStart() {
        try {
            StressTestConfig config = new StressTestConfig(
                    hostField.getText().trim(),
                    parseInt(portField, "Port", 1, 65535),
                    prefixField.getText().trim(),
                    parseInt(startField, "Start Number", 1, Integer.MAX_VALUE),
                    parseInt(botsField, "Bot Count", 1, 200_000),
                    parseDouble(rateField, "Connects / sec", 0.1D, 10_000D),
                    passwordField.getText(),
                    parseInt(keepAliveField, "KeepAlive (sec)", 1, 300),
                    parseInt(timeoutField, "Connect Timeout (ms)", 100, 120_000),
                    317,
                    false,
                    false
            );

            if (config.getUsernamePrefix().isEmpty()) {
                throw new IllegalArgumentException("Username Prefix cannot be empty.");
            }
            if (config.getHost().isEmpty()) {
                throw new IllegalArgumentException("Host cannot be empty.");
            }

            controller.start(config, this::appendLog);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Invalid Configuration",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onStop() {
        controller.stop();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void refreshStats() {
        StressStatsSnapshot stats = controller.snapshot();
        runningValue.setText(Boolean.toString(stats.isRunning()));
        progressValue.setText(stats.getAttempted() + " / " + stats.getTargetBots());
        successValue.setText(Integer.toString(stats.getSucceeded()));
        failValue.setText(Integer.toString(stats.getFailed()));
        activeValue.setText(Integer.toString(stats.getActive()));
        disconnectedValue.setText(Integer.toString(stats.getDisconnectedAfterLogin()));
        avgConnectValue.setText(stats.getAvgConnectMs() + " ms");
        p95ConnectValue.setText(stats.getP95ConnectMs() + " ms");
        maxConnectValue.setText(stats.getMaxConnectMs() + " ms");
        uptimeValue.setText(formatDuration(stats.getUptimeMs()));

        if (!stats.isRunning()) {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message);
            logArea.append("\n");
            int maxChars = 300_000;
            if (logArea.getText().length() > maxChars) {
                logArea.replaceRange("", 0, 150_000);
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private int parseInt(JTextField field, String label, int min, int max) {
        int value;
        try {
            value = Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
        }
        return value;
    }

    private double parseDouble(JTextField field, String label, double min, double max) {
        double value;
        try {
            value = Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
        }
        return value;
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1_000L);
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
