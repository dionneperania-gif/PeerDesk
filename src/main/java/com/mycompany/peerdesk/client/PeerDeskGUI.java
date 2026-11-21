package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.model.Ticket;
import com.mycompany.peerdesk.protocol.Protocol;
import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.ChatBubbleRow;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import com.mycompany.peerdesk.ui.PillTextField;
import com.mycompany.peerdesk.ui.WhimsicalDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * PeerDeskGUI featuring a modern Messenger / Instagram DM style conversation feed.
 * Outgoing messages align right in soft blush pink bubbles;
 * Incoming messages align left in pale mint green bubbles with sender headers;
 * Bubbles automatically shrink to fit compact text or wrap naturally up to ~70% width.
 */
public class PeerDeskGUI extends JFrame implements PeerDeskClientListener {

    private final PeerDeskClient client;
    private final List<Ticket> currentTickets = new ArrayList<>();
    private final java.util.Map<Integer, ChatBubbleRow.BubblePanel> bubblePanels = new java.util.HashMap<>();

    // Connection components
    private PillTextField hostField;
    private PillTextField portField;
    private PillTextField nickField;
    private PillButton connectBtn;
    private JLabel statusBadge;

    // Messenger/Instagram DM Style Chat Feed
    private JPanel chatCard;
    private JPanel messagesFeed;
    private JScrollPane chatScrollPane;
    private PillTextField messageField;
    private PillButton sendBtn;

    // Online users components
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JLabel peersCountLabel;

    // Ticket board components
    private JTabbedPane rightTabs;
    private DefaultTableModel ticketTableModel;
    private JTable ticketTable;
    private PillButton newTicketBtn;
    private PillButton claimTicketBtn;
    private PillButton resolveTicketBtn;
    private PillButton deleteTicketBtn;

    public PeerDeskGUI() {
        this.client = new PeerDeskClient();
        this.client.setListener(this);
        initUI();
    }

    private void initUI() {
        setTitle("PeerDesk | LAN Collaboration & Study Help Desk");
        setSize(1100, 740);
        setMinimumSize(new Dimension(940, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });

        // 1. Full-Window Botanical Background
        BotanicalBackgroundPanel rootPanel = new BotanicalBackgroundPanel(new BorderLayout(14, 14));
        rootPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // 2. Top Header Bar
        rootPanel.add(buildEditorialHeaderBar(), BorderLayout.NORTH);

        // 3. Fixed Center Container (No Draggable Splitter: Chat ~35-40%, Help Tickets ~60-65%)
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 14);

        // Left: Chat panel (~38% proportion)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.38;
        JPanel chatPanel = buildChatCard();
        chatPanel.setMinimumSize(new Dimension(340, 200));
        centerPanel.add(chatPanel, gbc);

        // Right: Help Tickets & Peers panel (~62% proportion)
        gbc.gridx = 1;
        gbc.weightx = 0.62;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel rightPanel = buildRightCard();
        rightPanel.setMinimumSize(new Dimension(480, 200));
        centerPanel.add(rightPanel, gbc);

        rootPanel.add(centerPanel, BorderLayout.CENTER);

        setContentPane(rootPanel);
        setupKeyBindings();
        updateConnectionState(false);
    }

    /**
     * Builds the Top Editorial Header Bar.
     */
    private JPanel buildEditorialHeaderBar() {
        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(14, 0), 24);
        card.setFillColor(BotanicalTheme.IVORY_CARD);

        // Brand section: Georgia Bold Display Title (26px) + Subtitle
        JPanel brandPanel = new JPanel(new BorderLayout(0, 3));
        brandPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("PeerDesk");
        logoLabel.setFont(BotanicalTheme.FONT_TITLE_MAIN);
        logoLabel.setForeground(BotanicalTheme.DEEP_ROSE);

        JLabel tagLabel = new JLabel("STUDENT LAN COLLABORATION & HELP DESK");
        tagLabel.setFont(BotanicalTheme.FONT_LABEL);
        tagLabel.setForeground(BotanicalTheme.MUTED_TEXT);

        brandPanel.add(logoLabel, BorderLayout.NORTH);
        brandPanel.add(tagLabel, BorderLayout.SOUTH);
        card.add(brandPanel, BorderLayout.WEST);

        // Inputs Section
        JPanel inputsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        inputsPanel.setOpaque(false);

        inputsPanel.add(createMiniLabel("HOST:"));
        hostField = new PillTextField("localhost", 9);
        hostField.setFont(BotanicalTheme.FONT_INPUT);
        inputsPanel.add(hostField);

        inputsPanel.add(createMiniLabel("PORT:"));
        portField = new PillTextField("8888", 4);
        portField.setFont(BotanicalTheme.FONT_INPUT);
        inputsPanel.add(portField);

        inputsPanel.add(createMiniLabel("PEER ID:"));
        nickField = new PillTextField("Peer" + (int)(Math.random() * 899 + 100), 8);
        nickField.setFont(BotanicalTheme.fontSans(14, Font.BOLD));
        inputsPanel.add(nickField);

        connectBtn = new PillButton("Connect", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        connectBtn.addActionListener(e -> toggleConnect());
        inputsPanel.add(connectBtn);

        statusBadge = new JLabel("  [OFFLINE]  ", SwingConstants.CENTER);
        statusBadge.setOpaque(true);
        statusBadge.setBackground(BotanicalTheme.BLUSH_LIGHT);
        statusBadge.setForeground(BotanicalTheme.DEEP_ROSE);
        statusBadge.setFont(BotanicalTheme.FONT_STATUS_BADGE);
        statusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        inputsPanel.add(statusBadge);

        card.add(inputsPanel, BorderLayout.EAST);
        return card;
    }

    /**
     * Builds the Messenger / Instagram DM Style Conversation Card.
     */
    private JPanel buildChatCard() {
        chatCard = new EditorialCardPanel(new BorderLayout(0, 10), 24);
        ((EditorialCardPanel) chatCard).setFillColor(BotanicalTheme.IVORY_CARD);

        // Header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setOpaque(false);

        JLabel title = new JLabel("Group Study & Discussion");
        title.setFont(BotanicalTheme.FONT_TITLE_SECTION);
        title.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        chatHeader.add(title, BorderLayout.WEST);

        PillButton helpBtn = new PillButton("Commands", BotanicalTheme.MINT_SOFT, BotanicalTheme.SAGE_DARK);
        helpBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        helpBtn.addActionListener(e -> showCommandsHelp());
        chatHeader.add(helpBtn, BorderLayout.EAST);

        chatCard.add(chatHeader, BorderLayout.NORTH);

        // Vertical Conversation Feed
        messagesFeed = new JPanel();
        messagesFeed.setLayout(new BoxLayout(messagesFeed, BoxLayout.Y_AXIS));
        messagesFeed.setOpaque(false);

        ScrollableFeedPanel feedWrapper = new ScrollableFeedPanel();
        feedWrapper.add(messagesFeed, BorderLayout.NORTH);

        // Clean welcome notice
        messagesFeed.add(createWelcomeRow());

        chatScrollPane = new JScrollPane(feedWrapper);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1));
        chatScrollPane.getViewport().setBackground(new Color(255, 253, 250));
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Responsive resizing listener for all chat bubbles
        chatScrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int vw = chatScrollPane.getViewport().getWidth();
                if (vw <= 0) return;
                int maxBubbleWidth = Math.max(160, (int) (vw * 0.72));
                for (ChatBubbleRow.BubblePanel bp : bubblePanels.values()) {
                    bp.setMaxBubbleWidth(maxBubbleWidth);
                }
                messagesFeed.revalidate();
                messagesFeed.repaint();
            }
        });

        chatCard.add(chatScrollPane, BorderLayout.CENTER);

        // Bottom Fixed Message Input Area
        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);

        messageField = new PillTextField(30);
        messageField.setPlaceholder("Type a message or /help...");
        messageField.setFont(BotanicalTheme.FONT_INPUT);
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        inputRow.add(messageField, BorderLayout.CENTER);

        sendBtn = new PillButton("Send", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        sendBtn.addActionListener(e -> sendMessage());
        inputRow.add(sendBtn, BorderLayout.EAST);

        chatCard.add(inputRow, BorderLayout.SOUTH);
        return chatCard;
    }

    private JPanel createWelcomeRow() {
        JPanel welcomeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        welcomeRow.setOpaque(false);
        JLabel welcomeLabel = new JLabel("Welcome to PeerDesk  -  Connect to chat and collaborate on help tickets");
        welcomeLabel.setFont(BotanicalTheme.fontSans(11, Font.ITALIC));
        welcomeLabel.setForeground(BotanicalTheme.MUTED_TEXT);
        welcomeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1),
                new EmptyBorder(4, 12, 4, 12)
        ));
        welcomeRow.add(welcomeLabel);
        return welcomeRow;
    }

    /**
     * Scrollable feed container that tracks viewport width strictly, preventing
     * right-aligned bubbles from being pushed outside or clipped by scrollbars.
     */
    private static class ScrollableFeedPanel extends JPanel implements Scrollable {
        public ScrollableFeedPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Builds the Right Card containing Help Tickets and Online Peers.
     */
    private JPanel buildRightCard() {
        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(), 24);
        card.setFillColor(BotanicalTheme.IVORY_CARD);

        rightTabs = new JTabbedPane();
        rightTabs.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        rightTabs.setBackground(BotanicalTheme.IVORY_CARD);
        rightTabs.setForeground(BotanicalTheme.CHARCOAL_TEXT);

        rightTabs.addTab("Help Tickets", buildTicketDeskPanel());
        rightTabs.addTab("Active Classmates", buildUserListPanel());

        card.add(rightTabs, BorderLayout.CENTER);
        return card;
    }

    /**
     * Help Tickets Table and Controls.
     */
    private JPanel buildTicketDeskPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 4, 4, 4));

        // Top Actions
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        actionsBar.setOpaque(false);

        newTicketBtn = new PillButton("+ New Ticket", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        newTicketBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        newTicketBtn.addActionListener(e -> openNewTicketDialog());
        actionsBar.add(newTicketBtn);

        claimTicketBtn = new PillButton("Assist", BotanicalTheme.SAGE_GREEN, Color.WHITE);
        claimTicketBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        claimTicketBtn.addActionListener(e -> handleClaimTicket());
        actionsBar.add(claimTicketBtn);

        PillButton statusBtn = new PillButton("Change Status", BotanicalTheme.MINT_SOFT, BotanicalTheme.FOREST_DEEP);
        statusBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        statusBtn.addActionListener(e -> handleChangeStatus());
        actionsBar.add(statusBtn);

        PillButton viewBtn = new PillButton("View Details", BotanicalTheme.PASTEL_LAVENDER, new Color(74, 45, 94));
        viewBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        viewBtn.addActionListener(e -> showSelectedTicketDetails());
        actionsBar.add(viewBtn);

        resolveTicketBtn = new PillButton("Mark Resolved", BotanicalTheme.LIME_ACCENT, BotanicalTheme.FOREST_DEEP);
        resolveTicketBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        resolveTicketBtn.addActionListener(e -> handleResolveTicket());
        actionsBar.add(resolveTicketBtn);

        deleteTicketBtn = new PillButton("Remove", BotanicalTheme.WARM_PEACH, BotanicalTheme.DEEP_ROSE);
        deleteTicketBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        deleteTicketBtn.addActionListener(e -> handleDeleteTicket());
        actionsBar.add(deleteTicketBtn);

        panel.add(actionsBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Status", "Category", "Problem / Topic", "Requester", "Helper"};
        ticketTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ticketTable = new JTable(ticketTableModel);
        ticketTable.setRowHeight(30);
        ticketTable.setFont(BotanicalTheme.fontSans(13, Font.PLAIN));
        ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ticketTable.getTableHeader().setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        ticketTable.getTableHeader().setBackground(BotanicalTheme.BLUSH_LIGHT);
        ticketTable.getTableHeader().setForeground(BotanicalTheme.DEEP_ROSE);

        ticketTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        ticketTable.getColumnModel().getColumn(1).setPreferredWidth(105);
        ticketTable.getColumnModel().getColumn(2).setPreferredWidth(105);
        ticketTable.getColumnModel().getColumn(3).setPreferredWidth(155);
        ticketTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        ticketTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        // Status Renderer
        ticketTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (column == 1 && value != null) {
                        String status = value.toString();
                        if ("OPEN".equals(status)) {
                            c.setBackground(BotanicalTheme.BLUSH_PINK);
                            c.setForeground(BotanicalTheme.DEEP_ROSE);
                        } else if ("IN_PROGRESS".equals(status)) {
                            c.setBackground(BotanicalTheme.PASTEL_YELLOW);
                            c.setForeground(BotanicalTheme.PASTEL_AMBER);
                        } else if ("RESOLVED".equals(status)) {
                            c.setBackground(BotanicalTheme.MINT_SOFT);
                            c.setForeground(BotanicalTheme.FOREST_DEEP);
                        }
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(254, 250, 252));
                        c.setForeground(BotanicalTheme.CHARCOAL_TEXT);
                    }
                }
                return c;
            }
        });

        // Double-click row to view full details
        ticketTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedTicketDetails();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(ticketTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1));
        tableScroll.getViewport().setBackground(Color.WHITE);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Active Classmates Panel.
     */
    private JPanel buildUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 4, 4, 4));

        peersCountLabel = new JLabel("Active Classmates (0)");
        peersCountLabel.setFont(BotanicalTheme.FONT_TITLE_SECTION);
        peersCountLabel.setForeground(BotanicalTheme.DEEP_ROSE);
        panel.add(peersCountLabel, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(BotanicalTheme.fontSans(14, Font.PLAIN));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(new Color(255, 253, 250));

        userList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel("  [Peer]  " + value);
            lbl.setFont(BotanicalTheme.fontSans(14, Font.PLAIN));
            lbl.setOpaque(true);
            if (isSelected) {
                lbl.setBackground(BotanicalTheme.BLUSH_PINK);
                lbl.setForeground(BotanicalTheme.DEEP_ROSE);
            } else {
                lbl.setBackground(index % 2 == 0 ? Color.WHITE : new Color(254, 250, 252));
                lbl.setForeground(BotanicalTheme.CHARCOAL_TEXT);
            }
            lbl.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            return lbl;
        });

        JScrollPane scroll = new JScrollPane(userList);
        scroll.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1));
        scroll.getViewport().setBackground(new Color(255, 253, 250));
        panel.add(scroll, BorderLayout.CENTER);

        PillButton renameBtn = new PillButton("Rename Yourself (/name)", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.DEEP_ROSE);
        renameBtn.setFont(BotanicalTheme.FONT_BUTTON);
        renameBtn.addActionListener(e -> promptRename());
        panel.add(renameBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void showCommandsHelp() {
        new CommandsHelpDialog(this);
    }

    private void promptRename() {
        String newName = JOptionPane.showInputDialog(this, "Enter your new nickname:", "Rename Peer", JOptionPane.PLAIN_MESSAGE);
        if (newName != null && !newName.trim().isEmpty()) {
            client.sendChat("/name " + newName.trim());
        }
    }

    // --- Action Handlers ---

    private void toggleConnect() {
        if (client.isConnected()) {
            client.disconnect("User clicked Disconnect.");
            updateConnectionState(false);
        } else {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            String nick = nickField.getText().trim();

            if (host.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Server IP address cannot be blank.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int port;
            try {
                port = Integer.parseInt(portStr);
                if (port < 1 || port > 65535) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Port must be a number between 1 and 65535.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (nick.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nickname cannot be blank.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            statusBadge.setText("  [CONNECTING]  ");
            statusBadge.setBackground(BotanicalTheme.PASTEL_YELLOW);
            statusBadge.setForeground(BotanicalTheme.PASTEL_AMBER);
            connectBtn.setEnabled(false);

            client.connectAsync(host, port, nick);
        }
    }

    private void sendMessage() {
        String msg = messageField.getText();
        if (msg == null || msg.trim().isEmpty()) return;
        client.sendChat(msg);
        messageField.setText("");
    }

    private void openNewTicketDialog() {
        if (!client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Please connect to the server first.", "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new NewTicketDialog(this, client);
    }

    private void handleClaimTicket() {
        int selectedRow = ticketTable.getSelectedRow();
        if (selectedRow == -1) {
            WhimsicalDialog.showNotice(this, "Select a Ticket", "Please select a ticket from the table first to assist with.");
            return;
        }
        int ticketId = (int) ticketTableModel.getValueAt(selectedRow, 0);
        client.claimTicket(ticketId);
    }

    private void handleChangeStatus() {
        int selectedRow = ticketTable.getSelectedRow();
        if (selectedRow == -1) {
            WhimsicalDialog.showNotice(this, "Select a Ticket", "Please select a ticket from the table first to update its status.");
            return;
        }
        int ticketId = (int) ticketTableModel.getValueAt(selectedRow, 0);
        String currentStatus = (String) ticketTableModel.getValueAt(selectedRow, 1);
        new ChangeStatusDialog(this, client, ticketId, currentStatus).setVisible(true);
    }

    private void showSelectedTicketDetails() {
        int selectedRow = ticketTable.getSelectedRow();
        if (selectedRow == -1) {
            WhimsicalDialog.showNotice(this, "Select a Ticket", "Please select a ticket from the table first to view its details.");
            return;
        }
        int ticketId = (int) ticketTableModel.getValueAt(selectedRow, 0);
        Ticket targetTicket = null;
        for (Ticket t : currentTickets) {
            if (t.getId() == ticketId) {
                targetTicket = t;
                break;
            }
        }
        if (targetTicket == null) {
            WhimsicalDialog.showNotice(this, "Not Found", "Ticket details could not be found.");
            return;
        }
        new TicketDetailsDialog(this, targetTicket, client).setVisible(true);
    }

    private void handleResolveTicket() {
        int selectedRow = ticketTable.getSelectedRow();
        if (selectedRow == -1) {
            WhimsicalDialog.showNotice(this, "Select a Ticket", "Please select a ticket from the table first to mark as resolved.");
            return;
        }
        int ticketId = (int) ticketTableModel.getValueAt(selectedRow, 0);
        client.resolveTicket(ticketId);
    }

    private void handleDeleteTicket() {
        int selectedRow = ticketTable.getSelectedRow();
        if (selectedRow == -1) {
            WhimsicalDialog.showNotice(this, "Select a Ticket", "Please select a ticket from the table first to remove.");
            return;
        }
        int ticketId = (int) ticketTableModel.getValueAt(selectedRow, 0);
        boolean confirm = WhimsicalDialog.showConfirm(this, "Remove Ticket", "Are you sure you want to remove ticket #" + ticketId + "?", "Remove");
        if (confirm) {
            client.deleteTicket(ticketId);
        }
    }

    private void handleWindowClose() {
        if (client.isConnected()) {
            client.disconnect("Application closed.");
        }
        dispose();
        System.exit(0);
    }

    private void updateConnectionState(boolean isConnected) {
        connectBtn.setEnabled(true);
        if (isConnected) {
            connectBtn.setText("Disconnect");
            connectBtn.setButtonColors(BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.DEEP_ROSE);

            statusBadge.setText("  [ONLINE]  ");
            statusBadge.setBackground(BotanicalTheme.MINT_SOFT);
            statusBadge.setForeground(BotanicalTheme.FOREST_DEEP);
            statusBadge.setBorder(BorderFactory.createLineBorder(BotanicalTheme.SAGE_GREEN, 1));

            hostField.setEnabled(false);
            portField.setEnabled(false);
            nickField.setEnabled(false);
            messageField.setEnabled(true);
            sendBtn.setEnabled(true);
            newTicketBtn.setEnabled(true);
            claimTicketBtn.setEnabled(true);
            resolveTicketBtn.setEnabled(true);
            deleteTicketBtn.setEnabled(true);
        } else {
            connectBtn.setText("Connect");
            connectBtn.setButtonColors(BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);

            statusBadge.setText("  [OFFLINE]  ");
            statusBadge.setBackground(BotanicalTheme.BLUSH_LIGHT);
            statusBadge.setForeground(BotanicalTheme.DEEP_ROSE);
            statusBadge.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1));

            hostField.setEnabled(true);
            portField.setEnabled(true);
            nickField.setEnabled(true);
            messageField.setEnabled(false);
            sendBtn.setEnabled(false);
            newTicketBtn.setEnabled(false);
            claimTicketBtn.setEnabled(false);
            resolveTicketBtn.setEnabled(false);
            deleteTicketBtn.setEnabled(false);
            userListModel.clear();
            peersCountLabel.setText("Active Classmates (0)");
            currentTickets.clear();
            ticketTableModel.setRowCount(0);

            // Clear chat messages on disconnect so previous session text does not remain
            if (messagesFeed != null) {
                messagesFeed.removeAll();
                messagesFeed.add(createWelcomeRow());
                messagesFeed.revalidate();
                messagesFeed.repaint();
            }
        }
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            messagesFeed.revalidate();
            messagesFeed.repaint();
            JScrollBar vBar = chatScrollPane.getVerticalScrollBar();
            vBar.setValue(vBar.getMaximum());
        });
    }

    // --- PeerDeskClientListener Callbacks (All arrive on EDT) ---

    @Override
    public void onConnected(String assignedNick) {
        updateConnectionState(true);
        nickField.setText(assignedNick);
        messagesFeed.add(ChatBubbleRow.createSystemRow(Protocol.getCurrentTimestamp(), "Connected to PeerDesk as " + assignedNick));
        scrollToBottom();
    }

    @Override
    public void onAuthFailed(String reason) {
        updateConnectionState(false);
        JOptionPane.showMessageDialog(this, reason, "Connection Failed", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onNameChangeSuccess(String newNick) {
        nickField.setText(newNick);
        messagesFeed.add(ChatBubbleRow.createSystemRow(Protocol.getCurrentTimestamp(), "Your nickname is now " + newNick));
        scrollToBottom();
    }

    @Override
    public void onNameChangeFailed(String reason) {
        JOptionPane.showMessageDialog(this, reason, "Nickname Change Failed", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void onChatMessage(int messageId, String timestamp, String sender, String message) {
        boolean isSelf = client.getCurrentNickname() != null && client.getCurrentNickname().equalsIgnoreCase(sender);
        int viewWidth = chatScrollPane.getViewport().getWidth();
        if (viewWidth <= 0) viewWidth = chatScrollPane.getWidth();
        int maxBubbleWidth = Math.max(160, (int) (viewWidth * 0.72));

        ChatBubbleRow.MessageRowResult result = ChatBubbleRow.createMessageRow(
                messageId, sender, timestamp, message, isSelf, maxBubbleWidth,
                (msgId, reactionType) -> client.sendReaction(msgId, reactionType)
        );
        bubblePanels.put(messageId, result.bubblePanel);
        messagesFeed.add(result.rowPanel);
        messagesFeed.revalidate();
        scrollToBottom();
    }

    @Override
    public void onMessageReactionUpdated(int messageId, String reactionType, int count) {
        ChatBubbleRow.BubblePanel bubble = bubblePanels.get(messageId);
        if (bubble != null) {
            bubble.setReactionCount(reactionType, count);
        }
    }

    @Override
    public void onSystemMessage(String timestamp, String message) {
        JPanel sysRow = ChatBubbleRow.createSystemRow(timestamp, message);
        messagesFeed.add(sysRow);
        scrollToBottom();
    }

    @Override
    public void onUserListUpdated(List<String> users) {
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user);
        }
        peersCountLabel.setText("Active Classmates (" + users.size() + ")");
    }

    @Override
    public void onTicketListUpdated(List<Ticket> tickets) {
        currentTickets.clear();
        currentTickets.addAll(tickets);

        // Check if any ticket has multiple helpers to adjust column header
        boolean hasMultipleHelpers = false;
        for (Ticket t : tickets) {
            String h = t.getHelper();
            if (h != null && h.contains(",")) {
                hasMultipleHelpers = true;
                break;
            }
        }
        ticketTable.getColumnModel().getColumn(5).setHeaderValue(hasMultipleHelpers ? "Helpers" : "Helper");
        ticketTable.getTableHeader().repaint();

        ticketTableModel.setRowCount(0);
        for (Ticket t : tickets) {
            ticketTableModel.addRow(new Object[]{
                    t.getId(),
                    t.getStatus().name(),
                    t.getCategory(),
                    t.getSubject(),
                    t.getRequester(),
                    t.getHelper()
            });
        }
    }

    @Override
    public void onServerMessage(String message, boolean isError) {
        JPanel noticeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        noticeRow.setOpaque(false);
        String formattedMessage = "<html><center>" + message.replace("\n", "<br>") + "</center></html>";
        JLabel lbl = new JLabel(formattedMessage);
        lbl.setFont(BotanicalTheme.fontSans(12, isError ? Font.BOLD : Font.PLAIN));
        lbl.setForeground(isError ? BotanicalTheme.DEEP_ROSE : BotanicalTheme.PASTEL_AMBER);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isError ? BotanicalTheme.BORDER_ROSE : BotanicalTheme.PASTEL_YELLOW, 1),
                new EmptyBorder(4, 12, 4, 12)
        ));
        lbl.setOpaque(true);
        lbl.setBackground(isError ? BotanicalTheme.BLUSH_LIGHT : BotanicalTheme.PASTEL_YELLOW);
        noticeRow.add(lbl);
        messagesFeed.add(noticeRow);
        scrollToBottom();
    }

    @Override
    public void onDisconnected(String reason) {
        updateConnectionState(false);
        messagesFeed.add(ChatBubbleRow.createSystemRow(Protocol.getCurrentTimestamp(), "Disconnected: " + reason));
        scrollToBottom();
    }

    private void setupKeyBindings() {
        // Ctrl+N: New Ticket
        KeyStroke ctrlN = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlN, "action_new_ticket");
        getRootPane().getActionMap().put("action_new_ticket", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openNewTicketDialog();
            }
        });

        // Ctrl+K: Focus Host Field
        KeyStroke ctrlK = KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlK, "action_focus_host");
        getRootPane().getActionMap().put("action_focus_host", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hostField != null && hostField.isEnabled()) {
                    hostField.requestFocusInWindow();
                }
            }
        });

        // Ctrl+T: Switch Tabs between Help Tickets and Active Classmates
        KeyStroke ctrlT = KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlT, "action_switch_tab");
        getRootPane().getActionMap().put("action_switch_tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rightTabs != null && rightTabs.getTabCount() > 1) {
                    int nextIdx = (rightTabs.getSelectedIndex() + 1) % rightTabs.getTabCount();
                    rightTabs.setSelectedIndex(nextIdx);
                }
            }
        });
    }

    private JLabel createMiniLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BotanicalTheme.FONT_LABEL);
        label.setForeground(BotanicalTheme.MUTED_TEXT);
        return label;
    }
}
