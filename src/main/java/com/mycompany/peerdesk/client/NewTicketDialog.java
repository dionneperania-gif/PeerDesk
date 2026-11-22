package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import com.mycompany.peerdesk.ui.PillTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Modal dialog for submitting a new peer help ticket.
 */
public class NewTicketDialog extends JDialog {

    public NewTicketDialog(JFrame parent, PeerDeskClient client) {
        super(parent, "Submit Help Request", true);
        setSize(480, 320);
        setLocationRelativeTo(parent);
        initContent(client);
        setVisible(true);
    }

    private void initContent(PeerDeskClient client) {
        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(12, 12, 12, 12));

        EditorialCardPanel formCard = new EditorialCardPanel(new GridBagLayout(), 18);
        formCard.setFillColor(BotanicalTheme.IVORY_CARD);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formCard.add(createMiniLabel("REQUESTER:"), gbc);
        gbc.gridx = 1;
        String defaultReq = (client != null && client.getCurrentNickname() != null) ? client.getCurrentNickname() : "";
        PillTextField requesterField = new PillTextField(defaultReq, 20);
        requesterField.setPlaceholder("e.g. Student Name");
        formCard.add(requesterField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formCard.add(createMiniLabel("CATEGORY:"), gbc);
        gbc.gridx = 1;
        String[] categories = {"Java / Sockets", "GUI / Swing", "Algorithms / Math", "Database / SQL", "Web / General IT"};
        JComboBox<String> catBox = new JComboBox<>(categories);
        catBox.setFont(BotanicalTheme.FONT_INPUT);
        formCard.add(catBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formCard.add(createMiniLabel("PROBLEM TOPIC:"), gbc);
        gbc.gridx = 1;
        PillTextField subjectField = new PillTextField(20);
        subjectField.setPlaceholder("e.g. NullPointerException");
        formCard.add(subjectField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        PillButton submitBtn = new PillButton("Submit Ticket", BotanicalTheme.BUBBLEGUM_PINK, Color.WHITE);
        submitBtn.addActionListener(e -> {
            String requester = requesterField.getText().trim();
            String subject = subjectField.getText().trim();
            if (requester.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter the requester name.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (subject.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a short problem description.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            client.createTicket((String) catBox.getSelectedItem(), subject, requester);
            dispose();
        });

        PillButton cancelBtn = new PillButton("Cancel", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.MUTED_TEXT);
        cancelBtn.addActionListener(e -> dispose());

        btnPanel.add(cancelBtn);
        btnPanel.add(submitBtn);
        formCard.add(btnPanel, gbc);

        bg.add(formCard, BorderLayout.CENTER);
        setContentPane(bg);
    }

    private JLabel createMiniLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BotanicalTheme.FONT_LABEL);
        label.setForeground(BotanicalTheme.MUTED_TEXT);
        return label;
    }
}
