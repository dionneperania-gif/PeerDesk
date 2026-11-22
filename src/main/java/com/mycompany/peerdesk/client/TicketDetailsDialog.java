package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.model.Ticket;
import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Whimsical styled modal dialog displaying full details of a help ticket,
 * with an interactive "Edit Details" feature to update category, topic, and requester.
 */
public class TicketDetailsDialog extends JDialog {

    private final Ticket ticket;
    private final PeerDeskClient client;

    private JLabel reqVal;
    private JLabel catVal;
    private JLabel subText;

    public TicketDetailsDialog(JFrame parent, Ticket ticket) {
        this(parent, ticket, null);
    }

    public TicketDetailsDialog(JFrame parent, Ticket ticket, PeerDeskClient client) {
        super(parent, "Ticket #" + ticket.getId() + " Overview", true);
        this.ticket = ticket;
        this.client = client;

        setSize(520, 460);
        setLocationRelativeTo(parent);
        setResizable(false);
        initContent();
    }

    private void initContent() {
        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(16, 16, 16, 16));

        EditorialCardPanel card = new EditorialCardPanel(new BorderLayout(0, 14), 20);
        card.setFillColor(BotanicalTheme.IVORY_CARD);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Header Title & Status Badge
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Ticket #" + ticket.getId() + " Details");
        title.setFont(BotanicalTheme.FONT_TITLE_SECTION);
        title.setForeground(BotanicalTheme.FOREST_DEEP);
        header.add(title, BorderLayout.WEST);

        // Status badge pill
        JLabel statusPill = new JLabel("  " + ticket.getStatus().name() + "  ");
        statusPill.setFont(BotanicalTheme.fontSans(11, Font.BOLD));
        statusPill.setOpaque(true);
        if (ticket.getStatus() == Ticket.Status.OPEN) {
            statusPill.setBackground(BotanicalTheme.BLUSH_PINK);
            statusPill.setForeground(BotanicalTheme.DEEP_ROSE);
        } else if (ticket.getStatus() == Ticket.Status.IN_PROGRESS) {
            statusPill.setBackground(BotanicalTheme.PASTEL_YELLOW);
            statusPill.setForeground(BotanicalTheme.PASTEL_AMBER);
        } else {
            statusPill.setBackground(BotanicalTheme.MINT_SOFT);
            statusPill.setForeground(BotanicalTheme.FOREST_DEEP);
        }
        statusPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        header.add(statusPill, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Info Grid
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        infoPanel.add(createMiniLabel("REQUESTER:"), gbc);
        gbc.gridx = 1;
        String reqText = (ticket.getPeerId() != null && !ticket.getPeerId().equalsIgnoreCase(ticket.getRequester()))
                ? ticket.getRequester() + " (" + ticket.getPeerId() + ")"
                : ticket.getRequester();
        reqVal = new JLabel(reqText);
        reqVal.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        reqVal.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        infoPanel.add(reqVal, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        infoPanel.add(createMiniLabel("CATEGORY:"), gbc);
        gbc.gridx = 1;
        catVal = new JLabel(ticket.getCategory());
        catVal.setFont(BotanicalTheme.fontSans(13, Font.PLAIN));
        catVal.setForeground(BotanicalTheme.CHARCOAL_TEXT);
        infoPanel.add(catVal, gbc);

        String helperLabel = (ticket.getHelper() != null && ticket.getHelper().contains(",")) ? "HELPERS:" : "HELPER:";
        gbc.gridx = 0; gbc.gridy = 2;
        infoPanel.add(createMiniLabel(helperLabel), gbc);
        gbc.gridx = 1;
        JLabel helpVal = new JLabel(ticket.getHelper());
        helpVal.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        helpVal.setForeground(BotanicalTheme.SAGE_DARK);
        infoPanel.add(helpVal, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        infoPanel.add(createMiniLabel("SUBMITTED AT:"), gbc);
        gbc.gridx = 1;
        JLabel timeVal = new JLabel(ticket.getTimestamp());
        timeVal.setFont(BotanicalTheme.fontSans(12, Font.ITALIC));
        timeVal.setForeground(BotanicalTheme.MUTED_TEXT);
        infoPanel.add(timeVal, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        infoPanel.add(createMiniLabel("PROBLEM / TOPIC:"), gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        EditorialCardPanel subjectCard = new EditorialCardPanel(new BorderLayout(), 14);
        subjectCard.setFillColor(new Color(255, 245, 248));
        subjectCard.setBorderColor(BotanicalTheme.BORDER_ROSE);
        subjectCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        subText = new JLabel("<html><body style='width:340px; font-family:sans-serif; color:#2B2D42;'>"
                + ticket.getSubject() + "</body></html>");
        subText.setFont(BotanicalTheme.fontSans(13, Font.PLAIN));
        subjectCard.add(subText, BorderLayout.CENTER);
        infoPanel.add(subjectCard, gbc);

        card.add(infoPanel, BorderLayout.CENTER);

        // Action Buttons: Edit Details + Close
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        PillButton editBtn = new PillButton("Edit Details", BotanicalTheme.SAGE_GREEN, Color.WHITE);
        editBtn.setFont(BotanicalTheme.fontSans(13, Font.BOLD));
        editBtn.setEnabled(client != null && client.isConnected());
        editBtn.addActionListener(e -> {
            new EditTicketDialog(this, ticket, client, (newCat, newSub, newReq) -> {
                ticket.setCategory(newCat);
                ticket.setSubject(newSub);
                ticket.setRequester(newReq);
                catVal.setText(newCat);
                reqVal.setText(newReq);
                subText.setText("<html><body style='width:340px; font-family:sans-serif; color:#2B2D42;'>"
                        + newSub + "</body></html>");
            }).setVisible(true);
        });
        bottomBar.add(editBtn);

        PillButton closeBtn = new PillButton("Close", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn);

        card.add(bottomBar, BorderLayout.SOUTH);

        bg.add(card, BorderLayout.CENTER);
        setContentPane(bg);
    }

    private JLabel createMiniLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BotanicalTheme.FONT_LABEL);
        label.setForeground(BotanicalTheme.MUTED_TEXT);
        return label;
    }
}
