package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import Project.Client.Client;
import Project.Client.Interfaces.IMessageEvents;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.ITimeEvents;
import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.TimerType;

public class GameEventsView extends JPanel implements IPhaseEvent, IReadyEvent, IMessageEvents, ITimeEvents {
    private final JPanel content;
    private final boolean debugMode = true; // Set this to false to disable debugging styling
    private final JLabel timerText;
    private final GridBagConstraints gbcGlue = new GridBagConstraints();

    public GameEventsView() {
        super(new BorderLayout(10, 10));
        content = new JPanel(new GridBagLayout());

        if (debugMode) {
            content.setBorder(BorderFactory.createLineBorder(Color.RED));
            content.setBackground(new Color(240, 240, 240));
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        if (debugMode) {
            scroll.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        } else {
            scroll.setBorder(BorderFactory.createEmptyBorder());
        }
        this.add(scroll, BorderLayout.CENTER);

        // Add vertical glue to push messages to the top
        gbcGlue.gridx = 0;
        gbcGlue.gridy = GridBagConstraints.RELATIVE;
        gbcGlue.weighty = 1.0;
        gbcGlue.fill = GridBagConstraints.BOTH;
        content.add(Box.createVerticalGlue(), gbcGlue);

        timerText = new JLabel();
        this.add(timerText, BorderLayout.NORTH);
        timerText.setVisible(false);
        Client.INSTANCE.registerCallback(this);
        // UCID: gb373
        // Date: 07/28/2025
        // Summary: The button for toggling the away status of the user.
        JButton awayButton = new JButton("Mark Away");

        awayButton.addActionListener(e -> {
            Client.INSTANCE.toggleAwayStatus();
            boolean newAway = Client.INSTANCE.isAway();
            awayButton.setText(newAway ? "Back" : "Mark Away");
        });

        this.add(awayButton, BorderLayout.SOUTH);
        awayButton.setVisible(true);

    }

    public void addText(String text) {
        SwingUtilities.invokeLater(() -> {
            JEditorPane textContainer = new JEditorPane("text/plain", text);
            textContainer.setEditable(false);
            if (debugMode) {
                textContainer.setBorder(BorderFactory.createLineBorder(Color.BLUE));
                textContainer.setBackground(new Color(255, 255, 200));
            } else {
                textContainer.setBorder(BorderFactory.createEmptyBorder());
                textContainer.setBackground(new Color(0, 0, 0, 0));
            }
            textContainer.setText(text);
            int width = content.getWidth() > 0 ? content.getWidth() : 200;
            Dimension preferredSize = textContainer.getPreferredSize();
            textContainer.setPreferredSize(new Dimension(width, preferredSize.height));
            // Remove glue if present
            int lastIdx = content.getComponentCount() - 1;
            if (lastIdx >= 0 && content.getComponent(lastIdx) instanceof Box.Filler) {
                content.remove(lastIdx);
            }
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = content.getComponentCount() - 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 5, 0);
            content.add(textContainer, gbc);
            content.add(Box.createVerticalGlue(), gbcGlue);
            content.revalidate();
            content.repaint();
            JScrollPane parentScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, content);
            if (parentScrollPane != null) {
                SwingUtilities.invokeLater(() -> {
                    JScrollBar vertical = parentScrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                });
            }
        });
    }

    @Override
    public void onReceivePhase(Phase phase) {
        addText(String.format("The current phase is %s", phase));
    }

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (isQuiet) {
            return;
        }
        String displayName = Client.INSTANCE.getDisplayNameFromId(clientId);
        addText(String.format("%s is %s", displayName, isReady ? "ready" : "not ready"));
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Handles incoming messages from the server and displays them in the
    // game panel.
    @Override
    public void onMessageReceive(long id, String message) {
        if (id == Constants.GAME_EVENT_CHANNEL) {// using -2 as an internal channel for GameEvents
            addText(message);
        }
    }

    @Override
    public void onTimerUpdate(TimerType timerType, int time) {
        if (time >= 0) {
            timerText.setText(String.format("%s timer: %s", timerType.name(), time));
        } else {
            timerText.setText(" ");
        }
        timerText.setVisible(true);
    }

    @Override
    public void onExtraOptionsToggle(boolean toggled) {
        // Implement logic if needed when extra options are toggled
    }

    @Override
    public void onExtraOptionsEnabled(boolean enabled) {
        System.out.println("onExtraOptionsEnabled called with: " + enabled);

        SwingUtilities.invokeLater(() -> {
            if (Client.INSTANCE.getPlayView() != null) {
                System.out.println("Updating PlayView with extraOptionsEnabled: " + enabled);
                Client.INSTANCE.getPlayView().setExtraOptionsEnabled(enabled);
            } else {
                System.out.println("PlayView is null!");
            }
        });
    }

    // UCID: gb373
    // Date: 07/28/2025
    // Summary: Handles the cooldown options toggle event.
    @Override
    public void onCooldownOptionsToggle(boolean toggled) {
        System.out.println("onCooldownOptionsToggle called with: " + toggled);

        SwingUtilities.invokeLater(() -> {
            if (Client.INSTANCE.getPlayView() != null) {
                System.out.println("Updating PlayView with cooldownOptionsEnabled: " + toggled);
                Client.INSTANCE.getPlayView().setCooldownOptionsEnabled(toggled);
            } else {
                System.out.println("PlayView is null!");
            }
        });
    }

    // UCID: gb373
    // Date: 07/28/2025
    // Summary: Handles the away status change event and sending a message to the panel about the status change.
    @Override
    public void onAwayStatusChange(long clientId, boolean isAway) {
        String name = Client.INSTANCE.getDisplayNameFromId(clientId);
        addText(name + (isAway ? " is away." : " is no longer away."));
    }

    @Override
    public void onAwayStatusToggle(boolean toggled) {
        System.out.println("onAwayStatusToggle called with: " + toggled);
    }

    // UCID: gb373
    // Date: 07/28/2025
    // Summary: Handles the spectator status change event and sending a message to the panel about the status change.
    @Override
    public void onSpectatorStatusChange(long clientId, boolean isSpectator) {
        String name = Client.INSTANCE.getDisplayNameFromId(clientId);
        addText(name + (isSpectator ? " is now spectating." : " is no longer spectating."));
    }
}
