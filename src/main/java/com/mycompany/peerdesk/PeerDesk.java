package com.mycompany.peerdesk;

import com.mycompany.peerdesk.client.PeerDeskGUI;
import com.mycompany.peerdesk.server.PeerDeskServer;
import com.mycompany.peerdesk.server.ServerGUI;
import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * Main Entry Point for PeerDesk.
 * Features an editorial botanical launcher with clean typography and zero broken symbols.
 */
public class PeerDesk {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (args.length > 0) {
            if ("--server".equalsIgnoreCase(args[0])) {
                SwingUtilities.invokeLater(() -> new ServerGUI().setVisible(true));
                return;
            } else if ("--client".equalsIgnoreCase(args[0])) {
                SwingUtilities.invokeLater(() -> new PeerDeskGUI().setVisible(true));
                return;
            } else if ("--headless-server".equalsIgnoreCase(args[0])) {
                int port = args.length > 1 ? Integer.parseInt(args[1]) : 8888;
                try {
                    PeerDeskServer server = new PeerDeskServer(port);
                    server.start();
                    System.out.println("Headless PeerDesk Server running on port " + port);
                } catch (Exception e) {
                    System.err.println("Failed to start server: " + e.getMessage());
                }
                return;
            }
        }

        // Show Launch Selector Window
        SwingUtilities.invokeLater(PeerDesk::showLauncherWindow);
    }

    private static void showLauncherWindow() {
        JFrame frame = new JFrame("PeerDesk Launcher");
        frame.setSize(540, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BotanicalBackgroundPanel root = new BotanicalBackgroundPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(14, 18), 26);
        card.setFillColor(BotanicalTheme.IVORY_CARD);

        // Header Title: 26px Georgia Bold
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 4, 4));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("PeerDesk", SwingConstants.CENTER);
        title.setFont(BotanicalTheme.FONT_TITLE_MAIN);
        title.setForeground(BotanicalTheme.DEEP_ROSE);

        JLabel subtitle = new JLabel("STUDENT LAN CHAT & PEER HELP DESK", SwingConstants.CENTER);
        subtitle.setFont(BotanicalTheme.FONT_LABEL);
        subtitle.setForeground(BotanicalTheme.MUTED_TEXT);

        titlePanel.add(title);
        titlePanel.add(subtitle);
        card.add(titlePanel, BorderLayout.NORTH);

        // Buttons Panel with 14px Bold typography
        JPanel btnPanel = new JPanel(new GridLayout(3, 1, 12, 12));
        btnPanel.setOpaque(false);

        PillButton clientBtn = new PillButton("Join as Client (Chat & Tickets)", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        clientBtn.setPreferredSize(new Dimension(320, 46));
        clientBtn.addActionListener(e -> {
            frame.dispose();
            new PeerDeskGUI().setVisible(true);
        });

        PillButton serverBtn = new PillButton("Host Server (Control Panel)", BotanicalTheme.SAGE_GREEN, Color.WHITE);
        serverBtn.setPreferredSize(new Dimension(320, 46));
        serverBtn.addActionListener(e -> {
            frame.dispose();
            new ServerGUI().setVisible(true);
        });

        PillButton bothBtn = new PillButton("Launch Both (Server & Client)", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.DEEP_ROSE);
        bothBtn.setPreferredSize(new Dimension(320, 46));
        bothBtn.addActionListener(e -> {
            frame.dispose();
            new ServerGUI().setVisible(true);
            new PeerDeskGUI().setVisible(true);
        });

        btnPanel.add(clientBtn);
        btnPanel.add(serverBtn);
        btnPanel.add(bothBtn);
        card.add(btnPanel, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.setVisible(true);
    }
}
