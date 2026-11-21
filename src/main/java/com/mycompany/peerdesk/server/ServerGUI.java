package com.mycompany.peerdesk.server;

import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import com.mycompany.peerdesk.ui.PillTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * ServerGUI featuring clean editorial typography and robust font hierarchies.
 * All unsupported emojis and symbols have been completely replaced with
 * legible text to eliminate empty square (□) glyphs.
 */
public class ServerGUI extends JFrame implements ServerEventListener {

    private PeerDeskServer server;
    private PillTextField portField;
    private PillButton toggleBtn;
    private JLabel statusBadge;
    private JLabel activeClientsLabel;
    private JTextArea logArea;

    public ServerGUI() {
        initUI();
    }

    private void initUI() {
        setTitle("PeerDesk Server | Host Control Panel");
        setSize(820, 580);
        setMinimumSize(new Dimension(640, 460));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dispose();
                if (server != null && server.isRunning()) {
                    new Thread(() -> {
                        if (server != null) {
                            server.stop();
                            server = null;
                        }
                    }, "Server-Shutdown-On-Close").start();
                }
            }
        });

        BotanicalBackgroundPanel root = new BotanicalBackgroundPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Top Header & Controls Card
        root.add(buildTopCard(), BorderLayout.NORTH);

        // Center Log Card
        root.add(buildLogCard(), BorderLayout.CENTER);

        // Bottom LAN IP Info Card
        root.add(buildLanCard(), BorderLayout.SOUTH);

        setContentPane(root);

        // Auto-start server on port 8888
        startServer(8888);
    }

    private JPanel buildTopCard() {
        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(12, 0), 22);
        card.setFillColor(BotanicalTheme.IVORY_CARD);

        // Title section
        JPanel left = new JPanel(new BorderLayout(0, 3));
        left.setOpaque(false);
        JLabel title = new JLabel("PeerDesk Server");
        title.setFont(BotanicalTheme.FONT_TITLE_SECTION);
        title.setForeground(BotanicalTheme.SAGE_DARK);

        JLabel subtitle = new JLabel("LAN SERVER CONTROLLER");
        subtitle.setFont(BotanicalTheme.FONT_LABEL);
        subtitle.setForeground(BotanicalTheme.MUTED_TEXT);

        left.add(title, BorderLayout.NORTH);
        left.add(subtitle, BorderLayout.SOUTH);
        card.add(left, BorderLayout.WEST);

        // Right controls: Port, Toggle Button, Status Badge, Client Count
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        right.setOpaque(false);

        JLabel portLbl = new JLabel("PORT:");
        portLbl.setFont(BotanicalTheme.FONT_LABEL);
        portLbl.setForeground(BotanicalTheme.MUTED_TEXT);
        right.add(portLbl);

        portField = new PillTextField("8888", 4);
        portField.setFont(BotanicalTheme.FONT_INPUT);
        right.add(portField);

        toggleBtn = new PillButton("Stop Server", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        toggleBtn.addActionListener(e -> handleToggle());
        right.add(toggleBtn);

        statusBadge = new JLabel("  [ACTIVE]  ", SwingConstants.CENTER);
        statusBadge.setOpaque(true);
        statusBadge.setBackground(BotanicalTheme.MINT_SOFT);
        statusBadge.setForeground(BotanicalTheme.FOREST_DEEP);
        statusBadge.setFont(BotanicalTheme.FONT_STATUS_BADGE);
        statusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BotanicalTheme.SAGE_GREEN, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        right.add(statusBadge);

        activeClientsLabel = new JLabel("Connected Peers: 0");
        activeClientsLabel.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        activeClientsLabel.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        right.add(activeClientsLabel);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildLogCard() {
        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(0, 10), 22);
        card.setFillColor(BotanicalTheme.IVORY_CARD);

        JLabel lbl = new JLabel("Real-Time Socket Traffic & Event Log");
        lbl.setFont(BotanicalTheme.FONT_TITLE_SECTION);
        lbl.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        card.add(lbl, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(255, 253, 250));
        logArea.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        logArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1));
        scroll.getViewport().setBackground(new Color(255, 253, 250));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildLanCard() {
        EditorialCardPanel card = new EditorialCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 4), 18);
        card.setFillColor(BotanicalTheme.IVORY_CARD);

        JLabel info = new JLabel("LAN Host IP(s) for your partner to connect: " + getDetectedLanIps());
        info.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        info.setForeground(BotanicalTheme.SAGE_DARK);
        card.add(info);

        return card;
    }

    private String getDetectedLanIps() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        if (sb.length() > 0) sb.append(" | ");
                        sb.append(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
        return sb.length() > 0 ? sb.toString() : "127.0.0.1 (localhost)";
    }

    private void handleToggle() {
        if (server != null && server.isRunning()) {
            toggleBtn.setEnabled(false);
            toggleBtn.setText("Stopping...");
            new Thread(() -> {
                try {
                    if (server != null) {
                        server.stop();
                        server = null;
                    }
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        updateServerState(false);
                        toggleBtn.setEnabled(true);
                    });
                }
            }, "Server-Stop-Worker").start();
        } else {
            try {
                int port = Integer.parseInt(portField.getText().trim());
                startServer(port);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Port must be an integer (1 - 65535).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startServer(int port) {
        toggleBtn.setEnabled(false);
        toggleBtn.setText("Starting...");
        new Thread(() -> {
            try {
                PeerDeskServer newServer = new PeerDeskServer(port);
                newServer.setListener(this);
                newServer.start();
                server = newServer;
                SwingUtilities.invokeLater(() -> {
                    updateServerState(true);
                    toggleBtn.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Could not start server on port " + port + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    updateServerState(false);
                    toggleBtn.setEnabled(true);
                });
            }
        }, "Server-Start-Worker").start();
    }

    private void updateServerState(boolean running) {
        if (running) {
            toggleBtn.setText("Stop Server");
            toggleBtn.setButtonColors(BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);

            statusBadge.setText("  [ACTIVE]  ");
            statusBadge.setBackground(BotanicalTheme.MINT_SOFT);
            statusBadge.setForeground(BotanicalTheme.FOREST_DEEP);
            portField.setEnabled(false);
        } else {
            toggleBtn.setText("Start Server");
            toggleBtn.setButtonColors(BotanicalTheme.SAGE_GREEN, Color.WHITE);

            statusBadge.setText("  [STOPPED]  ");
            statusBadge.setBackground(BotanicalTheme.BLUSH_LIGHT);
            statusBadge.setForeground(BotanicalTheme.DEEP_ROSE);
            portField.setEnabled(true);
            activeClientsLabel.setText("Connected Peers: 0");
        }
    }

    @Override
    public void onLog(String timestamp, String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onClientCountChanged(int activeCount) {
        SwingUtilities.invokeLater(() -> {
            activeClientsLabel.setText("Connected Peers: " + activeCount);
        });
    }
}
