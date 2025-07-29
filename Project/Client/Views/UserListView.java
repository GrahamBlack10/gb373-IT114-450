package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import Project.Client.Client;
import Project.Client.Interfaces.IConnectionEvents;
import Project.Client.Interfaces.IPointsEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Interfaces.ITurnEvent;
import Project.Common.Constants;
import Project.Common.LoggerUtil;

/**
 * UserListView represents a UI component that displays a list of users.
 */
public class UserListView extends JPanel
        implements IConnectionEvents, IRoomEvents, IReadyEvent, IPointsEvent, ITurnEvent {
    private final JPanel userListArea;
    private final GridBagConstraints lastConstraints; // Keep track of the last constraints for the glue
    private final HashMap<Long, UserListItem> userItemsMap; // Maintain a map of client IDs to UserListItems

    public UserListView() {
        super(new BorderLayout(10, 10));
        userItemsMap = new HashMap<>();

        JPanel content = new JPanel(new GridBagLayout());
        userListArea = content;

        JScrollPane scroll = new JScrollPane(userListArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.add(scroll, BorderLayout.CENTER);

        // Add vertical glue to push items to the top
        lastConstraints = new GridBagConstraints();
        lastConstraints.gridx = 0;
        lastConstraints.gridy = GridBagConstraints.RELATIVE;
        lastConstraints.weighty = 1.0;
        lastConstraints.fill = GridBagConstraints.VERTICAL;
        userListArea.add(Box.createVerticalGlue(), lastConstraints);
        Client.INSTANCE.registerCallback(this);
    }

    /**
     * Adds a user to the list.
     */
    private void addUserListItem(long clientId, String clientName) {
        SwingUtilities.invokeLater(() -> {
            if (userItemsMap.containsKey(clientId)) {
                LoggerUtil.INSTANCE.warning("User already in the list: " + clientName);
                return;
            }
            LoggerUtil.INSTANCE.info("Adding user to list: " + clientName);
            UserListItem userItem = new UserListItem(clientId, clientName);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = userListArea.getComponentCount() - 1;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 5);
            // Remove the last glue component if it exists
            if (lastConstraints != null) {
                int index = userListArea.getComponentCount() - 1;
                if (index > -1) {
                    userListArea.remove(index);
                }
            }
            userListArea.add(userItem, gbc);
            userListArea.add(Box.createVerticalGlue(), lastConstraints);
            userItemsMap.put(clientId, userItem);
            userListArea.revalidate();
            userListArea.repaint();
        });
    }

    /**
     * Removes a user from the list.
     */
    private void removeUserListItem(long clientId) {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Removing user list item for id " + clientId);
            try {
                UserListItem item = userItemsMap.remove(clientId);
                if (item != null) {
                    userListArea.remove(item);
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            } catch (Exception e) {
                LoggerUtil.INSTANCE.severe("Error removing user list item", e);
            }
        });
    }

    /**
     * Clears the user list.
     */
    private void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Clearing user list");
            try {
                userItemsMap.clear();
                userListArea.removeAll();
                userListArea.revalidate();
                userListArea.repaint();
            } catch (Exception e) {
                LoggerUtil.INSTANCE.severe("Error clearing user list", e);
            }
        });
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // unused
    }

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            clearUserList();
            return;
        }
        String displayName = Client.INSTANCE.getDisplayNameFromId(clientId);
        if (isJoin) {
            addUserListItem(clientId, displayName);
        } else {
            removeUserListItem(clientId);
        }
    }

    @Override
    public void onClientDisconnect(long clientId) {
        removeUserListItem(clientId);
    }

    @Override
    public void onReceiveClientId(long id) {
        // unused
    }

    @Override
    public void onTookTurn(long clientId, boolean didtakeTurn) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.values().forEach(u -> u.setTurn(false));// reset all
            });
        } else if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setTurn(didtakeTurn);
            });
        }
    }

    // UCID: gb373
    // Date: 07/22/2025
    // Summary: Updates the points for a user in the user list.
    // This method is called when the server sends a points update.
    @Override
    public void onPointsUpdate(long clientId, int points) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            SwingUtilities.invokeLater(() -> {
                try {
                    userItemsMap.values().forEach(u -> u.setPoints(-1));
                    sortUserList();
                } catch (Exception e) {
                    LoggerUtil.INSTANCE.severe("Error resetting user items", e);
                }
            });
        } else if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                try {
                    userItemsMap.get(clientId).setPoints(points);
                    sortUserList();
                } catch (Exception e) {
                    LoggerUtil.INSTANCE.severe("Error setting user item", e);
                }
            });
        }
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Sorts the user list based on points and client names if there is a
    // tie.
    private void sortUserList() {
        try {
            // Get sorted list
            List<UserListItem> sortedItems = userItemsMap.values().stream()
                    .sorted((a, b) -> {
                        int cmp = Integer.compare(b.getPoints(), a.getPoints());
                        if (cmp == 0) {
                            return a.getClientName().compareToIgnoreCase(b.getClientName());
                        }
                        return cmp;
                    })
                    .toList();

            // Clear UI
            userListArea.removeAll();

            // Re-add sorted items
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 5);

            int y = 0;
            for (UserListItem item : sortedItems) {
                gbc.gridy = y++;
                userListArea.add(item, gbc);
            }

            // Re-add glue to push list to top
            gbc.gridy = y;
            gbc.weighty = 1.0;
            userListArea.add(Box.createVerticalGlue(), gbc);

            // Refresh UI
            userListArea.revalidate();
            userListArea.repaint();

        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Error sorting user list", e);
        }
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Handles pending pick updates and pick actions for users.
    public void onPendingUpdate(long clientId, boolean isPending) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> userItemsMap.get(clientId).setPendingPick(isPending));
        }
    }

    @Override
    public void onPendingPick(long clientId, boolean isPending) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> userItemsMap.get(clientId).setPendingPick(isPending));
        }
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Handles elimination status updates for users.
    @Override
    public void onEliminationStatus(long clientId, boolean isEliminated) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> userItemsMap.get(clientId).setEliminated(isEliminated));
        }
    }

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            SwingUtilities.invokeLater(() -> {
                try {
                    userItemsMap.values().forEach(u -> u.setTurn(false));// reset all
                } catch (Exception e) {
                    LoggerUtil.INSTANCE.severe("Error resetting user items", e);
                }
            });
        } else if (userItemsMap.containsKey(clientId)) {

            SwingUtilities.invokeLater(() -> {
                try {
                    LoggerUtil.INSTANCE.info("Setting user item ready for id " + clientId + " to " + isReady);
                    userItemsMap.get(clientId).setTurn(isReady, Color.GRAY);
                } catch (Exception e) {
                    LoggerUtil.INSTANCE.severe("Error setting user item", e);
                }
            });
        }
    }

    @Override
    public void onExtraOptionsEnabled(boolean enabled) {
    }

    @Override
    public void onExtraOptionsToggle(boolean enabled) {
    }

    @Override
    public void onCooldownOptionsToggle(boolean enabled) {
    }

    @Override
    public void onAwayStatusChange(long clientId, boolean isAway) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setAway(isAway);
            });
        }
    }

    @Override
    public void onAwayStatusToggle(boolean enabled) {
    }

    @Override
    public void onSpectatorStatusChange(long clientId, boolean isSpectator) {
        if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                UserListItem item = userItemsMap.get(clientId);
                item.setSpectator(isSpectator);
            });
        }
    }

}
