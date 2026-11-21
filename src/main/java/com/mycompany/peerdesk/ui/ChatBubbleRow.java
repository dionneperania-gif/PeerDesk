package com.mycompany.peerdesk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

/**
 * ChatBubbleRow builds Messenger / Instagram DM style compact chat bubbles
 * with live multi-client message reactions (Like, Helpful, Thanks, Agree).
 */
public class ChatBubbleRow {

    @FunctionalInterface
    public interface ReactionCallback {
        void onReact(int messageId, String reactionType);
    }

    @FunctionalInterface
    public interface TicketActionCallback {
        void onTicketAction(int ticketId, String action); // action: "view" or "claim"
    }

    private ChatBubbleRow() {}

    /**
     * Creates a Messenger / Instagram DM style message row with live reaction controls.
     */
    public static MessageRowResult createMessageRow(
            int messageId,
            String sender,
            String timestamp,
            String text,
            boolean isSelf,
            int maxBubbleWidth,
            ReactionCallback reactionCallback,
            TicketActionCallback ticketCallback
    ) {
        JPanel row = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 3));
        row.setOpaque(false);

        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        // Incoming: Display sender name cleanly above the bubble
        if (!isSelf) {
            JLabel nameLabel = new JLabel(sender);
            nameLabel.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
            nameLabel.setForeground(BotanicalTheme.SAGE_DARK);
            nameLabel.setBorder(new EmptyBorder(0, 6, 2, 0));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            col.add(nameLabel);
        }

        // Compact Bubble Component
        BubblePanel bubble = new BubblePanel(messageId, text, timestamp, isSelf, maxBubbleWidth, reactionCallback, ticketCallback);
        bubble.setAlignmentX(isSelf ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        col.add(bubble);

        row.add(col);
        return new MessageRowResult(row, bubble);
    }

    public static MessageRowResult createMessageRow(
            int messageId,
            String sender,
            String timestamp,
            String text,
            boolean isSelf,
            int maxBubbleWidth,
            ReactionCallback reactionCallback
    ) {
        return createMessageRow(messageId, sender, timestamp, text, isSelf, maxBubbleWidth, reactionCallback, null);
    }

    public static class MessageRowResult {
        public final JPanel rowPanel;
        public final BubblePanel bubblePanel;

        public MessageRowResult(JPanel rowPanel, BubblePanel bubblePanel) {
            this.rowPanel = rowPanel;
            this.bubblePanel = bubblePanel;
        }
    }

    /**
     * Creates a subtle centered system notice row, or a special announcement pill if it mentions a ticket.
     */
    public static JPanel createSystemRow(String timestamp, String message) {
        return createSystemRow(timestamp, message, null);
    }

    public static JPanel createSystemRow(String timestamp, String message, TicketActionCallback ticketCallback) {
        // Detect if this system message is announcing a ticket: e.g. "Alice submitted ticket #101: [Java] ..."
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("ticket #(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(message);
        if (m.find() && ticketCallback != null) {
            int ticketId = Integer.parseInt(m.group(1));
            return createTicketAnnouncementRow(timestamp, message, ticketId, ticketCallback);
        }

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        row.setOpaque(false);

        JLabel label = new JLabel(message + "  -  " + timestamp);
        label.setFont(BotanicalTheme.fontSans(11, Font.ITALIC));
        label.setForeground(BotanicalTheme.MUTED_TEXT);
        label.setBorder(new EmptyBorder(2, 10, 2, 10));

        row.add(label);
        return row;
    }

    private static JPanel createTicketAnnouncementRow(String timestamp, String message, int ticketId, TicketActionCallback ticketCallback) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        row.setOpaque(false);

        EditorialCardPanel pill = new EditorialCardPanel(new BorderLayout(8, 0), 16);
        pill.setFillColor(new Color(255, 243, 246));
        pill.setBorderColor(BotanicalTheme.BORDER_ROSE);
        pill.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel iconLbl = new JLabel("📢");
        iconLbl.setFont(BotanicalTheme.fontSans(12, Font.PLAIN));
        pill.add(iconLbl, BorderLayout.WEST);

        JLabel msgLbl = new JLabel(message + "  (" + timestamp + ")");
        msgLbl.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
        msgLbl.setForeground(BotanicalTheme.FOREST_DEEP);
        msgLbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        msgLbl.setToolTipText("Click to view ticket #" + ticketId);
        msgLbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ticketCallback.onTicketAction(ticketId, "view");
            }
        });
        pill.add(msgLbl, BorderLayout.CENTER);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionButtons.setOpaque(false);

        PillButton offerHelpBtn = new PillButton("Offer Help 🙋", BotanicalTheme.SAGE_GREEN, Color.WHITE);
        offerHelpBtn.setFont(BotanicalTheme.fontSans(11, Font.BOLD));
        offerHelpBtn.setPreferredSize(new Dimension(110, 26));
        offerHelpBtn.addActionListener(e -> ticketCallback.onTicketAction(ticketId, "claim"));
        actionButtons.add(offerHelpBtn);

        PillButton viewBtn = new PillButton("View", BotanicalTheme.BLUSH_LIGHT, BotanicalTheme.DEEP_ROSE);
        viewBtn.setFont(BotanicalTheme.fontSans(11, Font.BOLD));
        viewBtn.setPreferredSize(new Dimension(60, 26));
        viewBtn.addActionListener(e -> ticketCallback.onTicketAction(ticketId, "view"));
        actionButtons.add(viewBtn);

        pill.add(actionButtons, BorderLayout.EAST);
        row.add(pill);
        return row;
    }

    /**
     * Custom painted rounded message bubble with dynamic text wrapping
     * and live multi-client reaction pills.
     */
    public static class BubblePanel extends JPanel {
        private final int messageId;
        private final boolean isSelf;
        private final Color bubbleBg;
        private final Color bubbleBorder;
        private static final int CORNER_RADIUS = 18;

        private final JPanel reactionsPanel;
        private final Map<String, JLabel> reactionLabels = new HashMap<>();
        private final WrapLabel textLabel;

        public BubblePanel(
                int messageId,
                String text,
                String timestamp,
                boolean isSelf,
                int maxBubbleWidth,
                ReactionCallback reactionCallback
        ) {
            this(messageId, text, timestamp, isSelf, maxBubbleWidth, reactionCallback, null);
        }

        public BubblePanel(
                int messageId,
                String text,
                String timestamp,
                boolean isSelf,
                int maxBubbleWidth,
                ReactionCallback reactionCallback,
                TicketActionCallback ticketCallback
        ) {
            super(new BorderLayout(0, 4));
            this.messageId = messageId;
            this.isSelf = isSelf;

            this.bubbleBg = isSelf ? new Color(255, 230, 236) : new Color(232, 245, 233);
            this.bubbleBorder = isSelf ? BotanicalTheme.BORDER_ROSE : new Color(199, 233, 192);

            setOpaque(false);
            setBorder(new EmptyBorder(8, 14, 6, 14));

            // Check if message references tickets like "#101" or "ticket #101"
            java.util.regex.Matcher tm = java.util.regex.Pattern.compile("#(\\d+)").matcher(text != null ? text : "");
            java.util.List<Integer> mentionedTicketIds = new java.util.ArrayList<>();
            while (tm.find()) {
                try {
                    int tid = Integer.parseInt(tm.group(1));
                    if (!mentionedTicketIds.contains(tid)) {
                        mentionedTicketIds.add(tid);
                    }
                } catch (NumberFormatException ignored) {}
            }

            // 1. Message Text Label with dynamic pixel-accurate word wrap
            textLabel = new WrapLabel(text, maxBubbleWidth);
            textLabel.setFont(BotanicalTheme.FONT_CHAT);
            textLabel.setForeground(isSelf ? BotanicalTheme.DEEP_ROSE : BotanicalTheme.FOREST_DEEP);

            if (!mentionedTicketIds.isEmpty() && ticketCallback != null) {
                JPanel contentCol = new JPanel();
                contentCol.setLayout(new BoxLayout(contentCol, BoxLayout.Y_AXIS));
                contentCol.setOpaque(false);
                contentCol.add(textLabel);

                // Add clickable pill buttons for each mentioned ticket
                JPanel pillsRow = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 3));
                pillsRow.setOpaque(false);
                for (int tid : mentionedTicketIds) {
                    PillButton ticketPill = new PillButton("Ticket #" + tid + " ↗",
                            isSelf ? new Color(255, 215, 225) : BotanicalTheme.MINT_SOFT,
                            isSelf ? BotanicalTheme.DEEP_ROSE : BotanicalTheme.FOREST_DEEP);
                    ticketPill.setFont(BotanicalTheme.fontSans(10, Font.BOLD));
                    ticketPill.setPreferredSize(new Dimension(86, 22));
                    ticketPill.addActionListener(e -> ticketCallback.onTicketAction(tid, "view"));
                    pillsRow.add(ticketPill);
                }
                contentCol.add(pillsRow);
                add(contentCol, BorderLayout.CENTER);
            } else {
                add(textLabel, BorderLayout.CENTER);
            }

            // 2. Footer: Timestamp + React Action Link + Reactions Pills
            JPanel footer = new JPanel(new BorderLayout(8, 2));
            footer.setOpaque(false);

            // Left side: Timestamp
            JLabel timeLabel = new JLabel(timestamp);
            timeLabel.setFont(BotanicalTheme.fontSans(10, Font.PLAIN));
            timeLabel.setForeground(isSelf ? new Color(175, 110, 130) : new Color(110, 145, 120));
            footer.add(timeLabel, BorderLayout.WEST);

            // Right side: React Link & Reaction Pills
            JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            rightFooter.setOpaque(false);

            // Reactions Pills container
            reactionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            reactionsPanel.setOpaque(false);
            rightFooter.add(reactionsPanel);

            // React Link Button
            JLabel reactBtn = new JLabel("+ React");
            reactBtn.setFont(BotanicalTheme.fontSans(11, Font.BOLD));
            reactBtn.setForeground(isSelf ? BotanicalTheme.DEEP_ROSE : BotanicalTheme.SAGE_DARK);
            reactBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            reactBtn.setToolTipText("React to this message");

            // Reaction Popup Menu (Instagram / Messenger style options)
            JPopupMenu reactionMenu = new JPopupMenu();
            reactionMenu.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER_ROSE, 1));
            reactionMenu.setBackground(BotanicalTheme.IVORY_CARD);

            String[] options = {"Like", "Helpful", "Thanks", "Agree"};
            for (String opt : options) {
                JMenuItem item = new JMenuItem(opt);
                item.setFont(BotanicalTheme.fontSans(12, Font.BOLD));
                item.setBackground(BotanicalTheme.IVORY_CARD);
                item.setForeground(BotanicalTheme.CHARCOAL_TEXT);
                item.addActionListener(e -> {
                    if (reactionCallback != null) {
                        reactionCallback.onReact(messageId, opt);
                    }
                });
                reactionMenu.add(item);
            }

            reactBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    reactionMenu.show(reactBtn, e.getX(), e.getY());
                }
            });

            rightFooter.add(reactBtn);
            footer.add(rightFooter, BorderLayout.EAST);

            add(footer, BorderLayout.SOUTH);
        }

        /**
         * Dynamically updates the maximum allowed width when viewport resizes.
         */
        public void setMaxBubbleWidth(int maxW) {
            if (textLabel != null) {
                textLabel.setMaxAvailableWidth(maxW);
            }
            revalidate();
            repaint();
        }

        /**
         * Updates or creates a reaction pill on this bubble.
         */
        public void setReactionCount(String reactionType, int count) {
            JLabel pill = reactionLabels.get(reactionType);
            if (count > 0) {
                if (pill == null) {
                    pill = new JLabel(reactionType + " " + count);
                    pill.setFont(BotanicalTheme.fontSans(10, Font.BOLD));
                    pill.setForeground(BotanicalTheme.CHARCOAL_TEXT);
                    pill.setOpaque(true);
                    pill.setBackground(BotanicalTheme.PASTEL_YELLOW);
                    pill.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(230, 200, 100), 1),
                            new EmptyBorder(1, 6, 1, 6)
                    ));
                    reactionLabels.put(reactionType, pill);
                    reactionsPanel.add(pill);
                } else {
                    pill.setText(reactionType + " " + count);
                }
            } else if (pill != null) {
                reactionsPanel.remove(pill);
                reactionLabels.remove(reactionType);
            }
            reactionsPanel.revalidate();
            reactionsPanel.repaint();
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            g2.setColor(bubbleBg);
            g2.fillRoundRect(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS);

            g2.setColor(bubbleBorder);
            g2.drawRoundRect(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Custom lightweight wrapping label that calculates exact pixel widths and heights
     * dynamically, ensuring text wraps cleanly without HTML clipping or artificial width bloating.
     */
    public static class WrapLabel extends JLabel {
        private int maxAvailableWidth;

        public WrapLabel(String text, int maxAvailableWidth) {
            super(text != null ? text : "");
            this.maxAvailableWidth = Math.max(100, maxAvailableWidth);
        }

        public void setMaxAvailableWidth(int w) {
            int newW = Math.max(100, w);
            if (this.maxAvailableWidth != newW) {
                this.maxAvailableWidth = newW;
                revalidate();
                repaint();
            }
        }

        private List<String> computeLines(int targetW, FontMetrics fm) {
            List<String> result = new ArrayList<>();
            String raw = getText();
            if (raw == null || raw.isEmpty()) return result;

            String[] paragraphs = raw.split("\n", -1);
            for (String para : paragraphs) {
                if (para.isEmpty()) {
                    result.add("");
                    continue;
                }
                String[] words = para.split(" ");
                StringBuilder cur = new StringBuilder();
                for (String word : words) {
                    String test = cur.length() == 0 ? word : cur + " " + word;
                    if (fm.stringWidth(test) <= targetW) {
                        cur = new StringBuilder(test);
                    } else {
                        if (cur.length() > 0) {
                            result.add(cur.toString());
                        }
                        cur = new StringBuilder(word);
                    }
                }
                if (cur.length() > 0) {
                    result.add(cur.toString());
                }
            }
            return result;
        }

        @Override
        public Dimension getPreferredSize() {
            String raw = getText();
            if (raw == null || raw.isEmpty()) return new Dimension(0, 0);

            Font font = getFont();
            if (font == null) font = BotanicalTheme.FONT_CHAT;
            FontMetrics fm = getFontMetrics(font);

            int fullTextW = fm.stringWidth(raw);
            if (fullTextW <= maxAvailableWidth && !raw.contains("\n")) {
                return new Dimension(fullTextW + 4, fm.getHeight());
            }

            List<String> lines = computeLines(maxAvailableWidth, fm);
            int maxLineW = 0;
            for (String line : lines) {
                maxLineW = Math.max(maxLineW, fm.stringWidth(line));
            }
            int h = Math.max(fm.getHeight(), lines.size() * fm.getHeight() + Math.max(0, lines.size() - 1) * 3);
            return new Dimension(maxLineW + 4, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            String raw = getText();
            if (raw == null || raw.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont() != null ? getFont() : BotanicalTheme.FONT_CHAT);
            g2.setColor(getForeground());

            FontMetrics fm = g2.getFontMetrics();
            int w = getWidth() > 0 ? getWidth() : maxAvailableWidth;
            List<String> lines = computeLines(w, fm);

            int y = fm.getAscent();
            int lineHeight = fm.getHeight() + 3;
            for (String line : lines) {
                g2.drawString(line, 0, y);
                y += lineHeight;
            }
            g2.dispose();
        }
    }
}
