package com.mycompany.peerdesk.client;

import com.mycompany.peerdesk.model.Ticket;
import com.mycompany.peerdesk.ui.BotanicalBackgroundPanel;
import com.mycompany.peerdesk.ui.BotanicalTheme;
import com.mycompany.peerdesk.ui.EditorialCardPanel;
import com.mycompany.peerdesk.ui.PillButton;
import com.mycompany.peerdesk.ui.PillTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Modal dialog for editing an existing help ticket's details (Category, Subject/Topic, Requester).
 */
public class EditTicketDialog extends JDialog {

    @FunctionalInterface
    public interface OnTicketEditedCallback {
        void onEdited(String newCategory, String newSubject, String newRequester);
    }

    public EditTicketDialog(Window owner, Ticket ticket, PeerDeskClient client, OnTicketEditedCallback callback) {
        super(owner, "Edit Ticket #" + ticket.getId() + " Details", Dialog.ModalityType.APPLICATION_MODAL);
        setSize(480, 340);
        setLocationRelativeTo(owner);
        setResizable(false);
        initContent(ticket, client, callback);
    }

    private void initContent(Ticket ticket, PeerDeskClient client, OnTicketEditedCallback callback) {
        BotanicalBackgroundPanel bg = new BotanicalBackgroundPanel(new BorderLayout());
        bg.setBorder(new EmptyBorder(12, 12, 12, 12));

        EditorialCardPanel formCard = new EditorialCardPanel(new GridBagLayout(), 18);
        formCard.setFillColor(BotanicalTheme.IVORY_CARD);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel titleLbl = new JLabel("Edit Ticket #" + ticket.getId());
        titleLbl.setFont(BotanicalTheme.FONT_TITLE_SECTION);
        titleLbl.setForeground(BotanicalTheme.DEEP_ROSE);
        formCard.add(titleLbl, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        formCard.add(createMiniLabel("REQUESTER:"), gbc);
        gbc.gridx = 1;
        PillTextField requesterField = new PillTextField(ticket.getRequester(), 20);
        formCard.add(requesterField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formCard.add(createMiniLabel("CATEGORY:"), gbc);
        gbc.gridx = 1;
        String[] categories = {"Java / Sockets", "GUI / Swing", "Algorithms / Math", "Database / SQL", "Web / General IT"};
        JComboBox<String> catBox = new JComboBox<>(categories);
        catBox.setFont(BotanicalTheme.FONT_INPUT);
        catBox.setSelectedItem(ticket.getCategory());
        formCard.add(catBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formCard.add(createMiniLabel("PROBLEM TOPIC:"), gbc);
        gbc.gridx = 1;
        PillTextField subjectField = new PillTextField(ticket.getSubject(), 20);
        formCard.add(subjectField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        PillButton cancelBtn = new PillButton("Cancel", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.MUTED_TEXT);
        cancelBtn.addActionListener(e -> dispose());

        PillButton saveBtn = new PillButton("Save Changes", BotanicalTheme.SAGE_GREEN, Color.WHITE);
        saveBtn.addActionListener(e -> {
            String requester = requesterField.getText().trim();
            String subject = subjectField.getText().trim();
            String category = (String) catBox.getSelectedItem();

            if (requester.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Requester name cannot be blank.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (subject.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Problem description cannot be blank.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (client != null && client.isConnected()) {
                client.editTicket(ticket.getId(), category, subject, requester);
            }

            if (callback != null) {
                callback.onEdited(category, subject, requester);
            }

            dispose();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
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
