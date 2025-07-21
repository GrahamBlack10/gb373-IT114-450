package Project.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;
import Project.Common.TimerPayload;
import Project.Common.TimerType;
import Project.Common.PayloadType;
import Project.Common.Payload; // <-- Add this import for Payload
import Project.Exceptions.MissingCurrentPlayerException;
import Project.Exceptions.NotPlayersTurnException;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;

public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;
    private List<ServerThread> turnOrder = new ArrayList<>();
    private long currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
    private int round = 0;

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    // UCID: gb373
    // Date: 07/09/2025
    // Summary: Handles the addition of a new client to the GameRoom.
    // onClientRemoved is called when a client disconnects or leaves the room.
    // onClientAdded is called when a new client joins the room.
    @Override
    protected void onClientAdded(ServerThread sp) {
        // sync GameRoom state to new client

        syncCurrentPhase(sp);
        // sync only what's necessary for the specific phase
        // if you blindly sync everything, you'll get visual artifacts/discrepancies
        syncReadyStatus(sp);
        if (currentPhase != Phase.READY) {
            syncTurnStatus(sp); // turn/ready use the same visual process so ensure turn status is only called
                                // outside of ready phase
            syncPlayerPoints(sp);
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        long removedClient = sp.getClientId();
        turnOrder.removeIf(player -> player.getClientId() == sp.getClientId());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        } else if (removedClient == currentTurnClientId) {
            onTurnStart();
        }
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
            sendCurrentTime(TimerType.ROUND, -1);
        }
    }

    private void startTurnTimer() {
        turnTimer = new TimedEvent(30, () -> onTurnEnd());
        turnTimer.setTickCallback((time) -> {
            System.out.println("Turn Time: " + time);
            sendCurrentTime(TimerType.TURN, time);
        });
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
            sendCurrentTime(TimerType.TURN, -1);
        }
    }
    // end timer handlers

    // lifecycle methods

    /** {@inheritDoc} */
    // Ucid: gb373
    // Date: 07/09/2025
    // Summary: Handles the start of a session, initializing the game state and
    // notifying players.
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.IN_PROGRESS);
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
        setTurnOrder();
        round = 0;
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    // Ucid: gb373
    // Date: 07/09/2025
    // Summary: Handles the start of a round, resetting timers and notifying
    // players.
    // This method is called at the start of each round.
    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        resetRoundTimer();
        resetTurnStatus();
        resetPlayerChoices();
        changePhase(Phase.IN_PROGRESS);
        startRoundTimer();
        round++;
        // relay(null, String.format("Round %d has started", round));
        sendGameEvent(String.format("Round %d has started", round));
        // startRoundTimer(); Round timers aren't needed for turns
        // if you do decide to use it, ensure it's reasonable and based on the number of
        // players
        LoggerUtil.INSTANCE.info("onRoundStart() end");
        onTurnStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        try {
            ServerThread currentPlayer = getNextPlayer();
            // relay(null, String.format("It's %s's turn", currentPlayer.getDisplayName()));
            sendGameEvent(String.format("It's %s's turn", currentPlayer.getDisplayName()));
        } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {

            e.printStackTrace();
        }
        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring
        try {
            // optionally can use checkAllTookTurn();
            if (isLastPlayer()) {
                // if the current player is the last player in the turn order, end the round
                onRoundEnd();
            } else {
                onTurnStart();
            }
        } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {

            e.printStackTrace();
        }
        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    // Ucid: gb373
    // Date: 07/10/2025
    // Summary: Handles the end of a round, resetting game state and notifying
    // players.
    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer(); // reset timer if round ended without the time expiring

        resetRoundTimer();

        for (ServerThread player : clientsInRoom.values()) {
            if (!player.isEliminated() && player.getChoice() == null) {
                player.setEliminated(true);
            }
        }

        List<ServerThread> activePlayers = clientsInRoom.values().stream()
                .filter(p -> !p.isEliminated())
                .collect(Collectors.toList());

        if (activePlayers.size() < 2) {
            onSessionEnd();
            LoggerUtil.INSTANCE.info("onRoundEnd() end");
            if (round >= 3) {
                return;
            }
        }

        List<ServerThread> losers = new ArrayList<>();

        for (int i = 0; i < activePlayers.size(); i++) {
            ServerThread attacker = activePlayers.get(i);
            ServerThread defender = activePlayers.get((i + 1) % activePlayers.size());

            String attackerChoice = attacker.getChoice();
            String defenderChoice = defender.getChoice();

            if (attackerChoice == null || defenderChoice == null)
                continue;

            boolean attackerWins = (attackerChoice.equals("r") && defenderChoice.equals("s")) ||
                    (attackerChoice.equals("p") && defenderChoice.equals("r")) ||
                    (attackerChoice.equals("s") && defenderChoice.equals("p"));

            if (attackerWins) {
                attacker.setPoints(attacker.getPoints() + 1);
                relay(null, attacker.getDisplayName() + " (" + attackerChoice + ") beat " +
                        defender.getDisplayName() + " (" + defenderChoice + ")");
                losers.add(defender);
            } else if (!attackerChoice.equals(defenderChoice)) {
                relay(null, attacker.getDisplayName() + " (" + attackerChoice + ") lost to " +
                        defender.getDisplayName() + " (" + defenderChoice + ")");
            } else {
                relay(null, attacker.getDisplayName() + " (" + attackerChoice + ") tied with " +
                        defender.getDisplayName() + " (" + defenderChoice + ")");
            }
        }

        for (ServerThread loser : losers) {
            loser.setEliminated(true);
        }

        sendPointsStatusToAll();

        long survivors = clientsInRoom.values().stream()
                .filter(p -> !p.isEliminated())
                .count();

        if (survivors == 1) {
            relay(null, "Game Over! The Winner is " +
                    clientsInRoom.values().stream().filter(p -> !p.isEliminated()).findFirst().get().getDisplayName());
            onSessionEnd();
        } else if (survivors == 0) {
            relay(null, "Game Over! It's a tie!");
            onSessionEnd();
        } else {
            onRoundStart();
        }

        LoggerUtil.INSTANCE.info("onRoundEnd() end");
    }

    /** {@inheritDoc} */
    // Ucid: gb373
    // Date: 07/10/2025
    // Summary: Handles the end of the session, resetting game state and notifying
    // players.
    // This method is called when the game ends, either due to a single winner or a
    // tie.
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");

        List<ServerThread> remainingPlayers = clientsInRoom.values().stream()
                .filter(p -> !p.isEliminated())
                .collect(Collectors.toList());

        if (remainingPlayers.size() == 1) {
            relay(null, "Game Over! Winner is " + remainingPlayers.get(0).getDisplayName());
        } else {
            relay(null, "Game Over! It's a tie!");
        }

        List<ServerThread> scoreboard = clientsInRoom.values().stream()
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .collect(Collectors.toList());

        for (ServerThread player : scoreboard) {
            relay(null, player.getDisplayName() + ": " + player.getPoints() + " points");
        }

        clientsInRoom.values().forEach(player -> {
            player.setPoints(0);
            player.setEliminated(false);
            player.setChoice(null);
            player.setTookTurn(false);
            player.setReady(false);
            player.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Your game data has been reset. Please ready up for a new game.");
        });

        turnOrder.clear();
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;

        resetReadyStatus();
        resetTurnStatus();

        changePhase(Phase.READY);

        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }

    // end lifecycle methods

    // send/sync data to ServerThread(s)
    private void syncPlayerPoints(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendPoints(serverUser.getClientId(),
                        serverUser.getPoints());
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    private void sendPlayerPoints(ServerThread sp) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPoints(sp.getClientId(), sp.getPoints());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void sendResetTurnStatus() {
        clientsInRoom.values().forEach(spInRoom -> {
            boolean failedToSend = !spInRoom.sendResetTurnStatus();
            if (failedToSend) {
                removeClient(spInRoom);
            }
        });
    }
    private void sendToAllClients(Payload payload) {
        clientsInRoom.values().forEach(spInRoom -> {
            boolean failedToSend = !spInRoom.sendToClient(payload);
            if (failedToSend) {
                removeClient(spInRoom);
            }
        });
    }


    private void sendTurnStatus(ServerThread client, boolean tookTurn) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(client.getClientId(), client.didTakeTurn());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void resetPlayerChoices() {
        clientsInRoom.values().forEach(player -> player.setChoice(null));
    }

    private void startRoundTimer() {
        roundTimer = new TimedEvent(200, this::onRoundEnd); // Adjust time as needed
        roundTimer.setTickCallback(time -> {
            System.out.println("Round Time: " + time);

            // Build TimerPayload and send to all clients
            TimerPayload timerPayload = new TimerPayload();
            timerPayload.setPayloadType(PayloadType.TIME);
            timerPayload.setTimerType(TimerType.ROUND); // or the appropriate enum
            timerPayload.setTime(time);

            sendToAllClients(timerPayload); // Make sure you have this method to broadcast
        });
    }

    private void sendPointsStatusToAll() {
        clientsInRoom.values().forEach(client -> {
            int points = client.getPoints(); // get points from each client
            boolean failedToSend = !client.sendPoints(client.getClientId(), points);
            if (failedToSend) {
                removeClient(client);
            }
        });
    }

    private void sendPointsStatusToClient(ServerThread client) {
        clientsInRoom.values().forEach(otherClient -> {
            int points = otherClient.getPoints();
            boolean failedToSend = !client.sendPoints(otherClient.getClientId(), points);
            if (failedToSend) {
                removeClient(client);
            }
        });
    }

    private void syncTurnStatus(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendTurnStatus(serverUser.getClientId(),
                        serverUser.didTakeTurn(), true);
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    // end send data to ServerThread(s)

    // misc methods
    private void resetTurnStatus() {
        clientsInRoom.values().forEach(sp -> {
            sp.setTookTurn(false);
        });
        sendResetTurnStatus();
    }

    /**
     * Sets `turnOrder` to a shuffled list of players who are ready.
     */
    private void setTurnOrder() {
        turnOrder.clear();
        turnOrder = clientsInRoom.values().stream().filter(ServerThread::isReady).collect(Collectors.toList());
        Collections.shuffle(turnOrder);
    }

    private void broadcastMessageToRoom(String message) {
        clientsInRoom.values().forEach(player -> {
            boolean failedToSend = !player.sendMessage(Constants.DEFAULT_CLIENT_ID, message);
            if (failedToSend) {
                removeClient(player);
            }
        });
    }

    /**
     * Gets the current player based on the `currentTurnClientId`.
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private ServerThread getCurrentPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // quick early exit
        if (currentTurnClientId == Constants.DEFAULT_CLIENT_ID) {
            throw new MissingCurrentPlayerException("Current Player not set");
        }
        return turnOrder.stream()
                .filter(sp -> sp.getClientId() == currentTurnClientId)
                .findFirst()
                // this shouldn't occur but is included as a "just in case"
                .orElseThrow(() -> new PlayerNotFoundException("Current player not found in turn order"));
    }

    /**
     * Gets the next player in the turn order.
     * If the current player is the last in the turn order, it wraps around
     * (round-robin).
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private ServerThread getNextPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        int index = 0;
        if (currentTurnClientId != Constants.DEFAULT_CLIENT_ID) {
            index = turnOrder.indexOf(getCurrentPlayer()) + 1;
            if (index >= turnOrder.size()) {
                index = 0;
            }
        }
        ServerThread nextPlayer = turnOrder.get(index);
        currentTurnClientId = nextPlayer.getClientId();
        return nextPlayer;
    }

    /**
     * Checks if the current player is the last player in the turn order.
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private boolean isLastPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // check if the current player is the last player in the turn order
        return turnOrder.indexOf(getCurrentPlayer()) == (turnOrder.size() - 1);
    }

    private void checkAllTookTurn() {
        int numReady = clientsInRoom.values().stream()
                .filter(sp -> sp.isReady())
                .toList().size();
        int numTookTurn = clientsInRoom.values().stream()
                // ensure to verify the isReady part since it's against the original list
                .filter(sp -> sp.isReady() && sp.didTakeTurn())
                .toList().size();
        if (numReady == numTookTurn) {
            // relay(null,
            // String.format("All players have taken their turn (%d/%d) ending the round",
            // numTookTurn, numReady));
            sendGameEvent(
                    String.format("All players have taken their turn (%d/%d) ending the round", numTookTurn, numReady));
            onRoundEnd();
        }
    }

    // start check methods
    private void checkCurrentPlayer(long clientId) throws NotPlayersTurnException {
        if (currentTurnClientId != clientId) {
            throw new NotPlayersTurnException("You are not the current player");
        }
    }

    // end check methods

    // receive data from ServerThread (GameRoom specific)

    /**
     * Handles the turn action from the client.
     * 
     * @param currentUser
     * @param exampleText (arbitrary text from the client, can be used for
     *                    additional actions or information)
     */
    // Ucid: gb373
    // Date: 07/10/2025
    // Summary: Handles the turn action from the client, checking if the player is
    // ready, if it's their turn, and if they have already taken a turn.
    // If the player is ready and it's their turn, it processes the choice and
    // updates the game state accordingly.
    protected void handleTurnAction(ServerThread currentUser, String choice) {
        // check if the client is in the room
        try {
            if (currentUser.isEliminated()) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are eliminated and cannot take a turn.");
                return;
            }

            checkPlayerInRoom(currentUser);
            checkCurrentPhase(currentUser, Phase.IN_PROGRESS);
            checkCurrentPlayer(currentUser.getClientId());
            checkIsReady(currentUser);

            if (currentUser.didTakeTurn()) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have already taken your turn this round");
                return;
            }
            // example points
            int points = new Random().nextInt(4) == 3 ? 1 : 0;
            sendGameEvent(String.format("%s %s", currentUser.getDisplayName(),
                    points > 0 ? "gained a point" : "didn't gain a point"));
            if (points > 0) {
                currentUser.setPoints(points);
                sendPlayerPoints(currentUser);
            }
            if (!choice.equalsIgnoreCase("r") && !choice.equalsIgnoreCase("p") && !choice.equalsIgnoreCase("s")) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid choice. Please pick 'r', 'p', or 's'.");
                return;
            }

            // UCID: gb373
            // Date: 07/10/2025
            // Send/sync the choice to all clients in the room
            currentUser.setChoice(choice.toLowerCase());
            currentUser.setTookTurn(true);

            String message = String.format("%s picked their choice.", currentUser.getClientName());
            broadcastMessageToRoom(message);

            sendTurnStatus(currentUser, currentUser.didTakeTurn());

            onTurnEnd();

        } catch (NotPlayersTurnException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "It's not your turn");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (NotReadyException e) {
            // The check method already informs the currentUser
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PlayerNotFoundException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PhaseMismatchException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    String.format(
                            "You can only take a turn during the following phases: CHOOSING or IN_PROGRESS. Current phase: %s",
                            currentPhase.name()));
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        }
    }

    // end receive data from ServerThread (GameRoom specific)
}