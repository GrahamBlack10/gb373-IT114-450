package Project.Server;

import java.util.*;
import java.util.stream.Collectors;

import Project.Common.*;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;

public class GameRoom extends BaseGameRoom {
    private TimedEvent roundTimer = null;
    private int round = 0;
    private String roomName;
    private boolean extraOptionsEnabled = false;

    @Override
    protected void onTurnStart() {
        // Implement logic for when a player's turn starts, if needed.
        // For now, just send a game event.
        sendGameEvent("A player's turn has started.");
    }

    @Override
    protected void onTurnEnd() {
        // Implement logic for when a player's turn ends, if needed.
        // For now, just send a game event.
        sendGameEvent("A player's turn has ended.");
    }

    public GameRoom(String name) {
        super(name);
    }

    public String getRoomName() {
        return roomName;
    }

    // UCID: gb373
    // Date: 07/09/2025
    // Summary: Handles the addition of a new client to the GameRoom.
    // onClientRemoved is called when a client disconnects or leaves the room.
    // onClientAdded is called when a new client joins the room.
    @Override
    protected void onClientAdded(ServerThread sp) {
        syncCurrentPhase(sp);
        syncReadyStatus(sp);

        if (currentPhase != Phase.READY) {
            syncTurnStatus(sp);
            syncPlayerPoints(sp);
        }

        // UCID: gb373
        // Date: 07/24/2025
        // Summary: Setting the host status for the client.
        boolean isHost = clientsInRoom.size() == 1;
        Payload hostPayload = new Payload();
        hostPayload.setPayloadType(PayloadType.HOST_STATUS);
        hostPayload.setMessage(Boolean.toString(isHost));
        sp.sendToClient(hostPayload);

        handleReady(sp, true, false); // wantsSpectator = true, isReady = false
    }

    @Override
    protected void onClientRemoved(ServerThread sp) {
        clientsInRoom.remove(sp.getClientId());
        if (clientsInRoom.isEmpty()) {
            resetRoundTimer();
            onSessionEnd();
        }
    }

    @Override
    protected void onSessionStart() {
        clientsInRoom.values().forEach(player -> {
            player.setEliminated(false);
            player.sendEliminationStatus(player.getClientId(), false);
        });

        changePhase(Phase.IN_PROGRESS);
        round = 0;
        onRoundStart();
    }

    @Override
    protected void onRoundStart() {
        resetRoundTimer();
        resetPlayerChoices();

        // UCID: gb373
        // Date: 07/28/2025
        // Summary: Sync the pending status of players at the start of the round.
        // Away players are not considered pending.
        clientsInRoom.values().forEach(p -> {
            if (!p.isEliminated() && !p.isAway()) {
                clientsInRoom.values().forEach(other -> {
                    other.sendPendingStatus(p.getClientId(), true);
                });
            }
        });

        changePhase(Phase.IN_PROGRESS);
        round++;
        sendGameEvent("Round " + round + " has started! Use /pick r/p/s");

        // Start 30-second timer for the round
        roundTimer = new TimedEvent(30, this::onRoundEnd);
        roundTimer.setTickCallback(time -> sendCurrentTime(TimerType.ROUND, time));
    }

    // UCID: gb373
    // Date: 07/25/2025
    // Summary: Handles the end of a round, checking player choices and determining
    // winners.
    // If only one player remains, the game ends. If all players made choices, the
    // round ends and the next round starts.
    // If no players remain, the game ends in a tie.
    @Override
    protected void onRoundEnd() {
        resetRoundTimer();

        // Eliminate players who didn't choose
        // Away players are not eliminated for not picking.
        clientsInRoom.values().forEach(p -> {
            if (!p.isEliminated() && !p.isAway() && p.getChoice() == null && !p.isSpectator()) {
                p.setEliminated(true);
                sendGameEvent(p.getDisplayName() + " was eliminated for not picking.");
            }
        });

        List<ServerThread> active = clientsInRoom.values().stream()
                .filter(p -> !p.isEliminated() && !p.isSpectator() && p.getChoice() != null)
                .collect(Collectors.toList());

        Set<ServerThread> toEliminate = new HashSet<>();
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                ServerThread a = active.get(i);
                ServerThread b = active.get(j);

                if (toEliminate.contains(a) || toEliminate.contains(b))
                    continue;

                String choiceA = a.getChoice();
                String choiceB = b.getChoice();

                if (choiceA == null || choiceB == null)
                    continue;

                boolean aWins = winsAgainst(choiceA, choiceB);
                boolean bWins = winsAgainst(choiceB, choiceA);

                if (aWins && !bWins) {
                    a.setPoints(a.getPoints() + 1);
                    sendGameEvent(a.getDisplayName() + " (" + choiceA + ") beat " +
                            b.getDisplayName() + " (" + choiceB + ")");
                    toEliminate.add(b);
                } else if (bWins && !aWins) {
                    b.setPoints(b.getPoints() + 1);
                    sendGameEvent(b.getDisplayName() + " (" + choiceB + ") beat " +
                            a.getDisplayName() + " (" + choiceA + ")");
                    toEliminate.add(a);
                } else {
                    sendGameEvent(a.getDisplayName() + " (" + choiceA + ") tied with " +
                            b.getDisplayName() + " (" + choiceB + ")");
                }
            }
        }

        for (ServerThread p : toEliminate) {
            if (!p.isEliminated()) {
                p.setEliminated(true);
                sendGameEvent(p.getDisplayName() + " was eliminated.");
                broadcastEliminationStatus(p.getClientId(), true);
                p.sendEliminationStatus(p.getClientId(), true);
            }
        }

        // UCID: gb373
        // Date: 07/28/2025
        // Summary: Away players are not eliminated, but they can't win.
        long survivors = clientsInRoom.values().stream()
                .filter(p -> !p.isEliminated() && !p.isAway() && !p.isSpectator())
                .count();

        sendGameEvent("Survivors remaining: " + survivors);

        if (survivors == 1) {
            ServerThread winner = clientsInRoom.values().stream()
                    .filter(p -> !p.isEliminated())
                    .findFirst()
                    .get();
            sendGameEvent("Game Over! Winner: " + winner.getDisplayName());
            onSessionEnd();
        } else if (survivors == 0) {
            sendGameEvent("Game Over! It's a tie!");
            onSessionEnd();
        } else {
            onRoundStart();
        }

    }

    // UCID: gb373
    // Date: 07/25/2025
    // Summary: Handles the end of the session, notifying players of their points
    // and resetting the game state.
    @Override
    protected void onSessionEnd() {
        clientsInRoom.values().stream()
                .sorted(Comparator.comparingInt(ServerThread::getPoints).reversed())
                .forEach(p -> sendGameEvent(p.getDisplayName() + ": " + p.getPoints() + " points"));

        clientsInRoom.values().forEach(p -> {
            PointsPayload pp = new PointsPayload();
            pp.setPayloadType(PayloadType.POINTS);
            pp.setClientId(p.getClientId());
            pp.setPoints(p.getPoints());
            clientsInRoom.values().forEach(other -> other.sendToClient(pp));
        });

        clientsInRoom.values().forEach(p -> {
            if (p.isEliminated()) {
                broadcastEliminationStatus(p.getClientId(), true);
            }
        });

        clientsInRoom.values().forEach(player -> {
            player.setPoints(0);
            player.setChoice(null);
            player.setTookTurn(false);
            player.setEliminated(false);
            player.setReady(false);
            player.sendEliminationStatus(player.getClientId(), false);
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Game ended. Please /ready to start again.");
        });

        changePhase(Phase.READY);
    }

    // UCID: gb373
    // Date: 07/09/2025
    // Summary: Handles the player's turn action in the game in the GameRoom.
    protected void handleTurnAction(ServerThread player, String choice) {
        try {
            checkPlayerInRoom(player);
            checkIsReady(player);
            checkCurrentPhase(player, Phase.IN_PROGRESS);

            if (player.isEliminated()) {
                player.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are eliminated.");
                return;
            }

            if (player.getChoice() != null) {
                player.sendMessage(Constants.DEFAULT_CLIENT_ID, "You already picked.");
                return;
            }
            if (player.isAway()) {
                player.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are marked as away.");
                return;
            }
            if (player.isSpectator()) {
                player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot make moves.");
                return;
            }

            choice = choice.trim().toLowerCase();

            if (extraOptionsEnabled) {
                if (!choice.matches("[rpsfw]")) {
                    player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid choice. Use r, p, s, f, or w.");
                    return;
                }
            } else {
                if (!choice.matches("[rps]")) {
                    player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid choice. Use r, p, or s.");
                    return;
                }
            }

            player.setChoice(choice);
            player.setTookTurn(true);
            sendTurnStatus(player, true);
            sendGameEvent(player.getDisplayName() + " picked.");
            clientsInRoom.values().forEach(p -> p.sendPendingStatus(player.getClientId(), false));

            Payload confirm = new Payload();
            confirm.setPayloadType(PayloadType.TURN_CONFIRMED);
            player.sendToClient(confirm);

            // UCID: gb373
            // Date: 07/28/2025
            // Summary: Sync the player's points with all other players.
            // Ignores eliminated and away players.
            long remaining = clientsInRoom.values().stream()
                    .filter(p -> !p.isEliminated() && !p.isAway() && !p.isSpectator() && p.getChoice() == null)
                    .count();

            if (remaining == 0) {
                onRoundEnd();
            }

        } catch (PlayerNotFoundException e) {
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to play.");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (NotReadyException | PhaseMismatchException e) {
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "You can only pick during the game.");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Unexpected error in handleTurnAction", e);
        }
    }

    // UCID: gb373
    // Date: 07/28/2025
    // Summary: Handles the ready status of a player in the GameRoom.
    // If the player is ready, they are marked as ready and can start the game.
    // Spectators are not allowed to be ready.
    // If the player is not ready, they are marked as a spectator.
    // If the game is not in the READY phase, the player is marked as a spectator.
    // If the player is already ready, they are not marked as a spectator.
    protected void handleReady(ServerThread player, boolean wantsSpectator, boolean isReady) {

        if (currentPhase != Phase.READY) {
            player.setSpectator(true);
            player.setReady(false);
            player.setEliminated(false);
            player.setAway(false);
            sendGameEvent(player.getDisplayName() + " joined late as a spectator.");
            broadcastSpectatorStatus(player.getClientId(), true);

            return;
        }

        if (!isReady) {
            player.setSpectator(true);
            player.setReady(false);
            player.setEliminated(false);
            player.setAway(false);
            sendGameEvent(player.getDisplayName() + " joined as a spectator.");
            broadcastSpectatorStatus(player.getClientId(), true);

        } else {
            player.setSpectator(false);
            player.setReady(true);
            player.setEliminated(false);
            player.setAway(false);
            sendReadyStatus(player, true);
            broadcastSpectatorStatus(player.getClientId(), false);

        }

        // Continue existing check if all active players are ready
        List<ServerThread> activePlayers = clientsInRoom.values().stream()
                .filter(p -> !p.isSpectator())
                .collect(Collectors.toList());

        boolean allReady = activePlayers.stream().allMatch(p -> p.isReady() && !p.isSpectator());

        if (allReady && activePlayers.size() >= 2) {
            onSessionStart();
        } else if (allReady) {
            sendGameEvent("At least 2 active players are required to start the game.");
        }
    }

    private void resetPlayerChoices() {
        clientsInRoom.values().forEach(p -> {
            p.setChoice(null);
            p.setTookTurn(false);
            if (!p.isEliminated()) {
                broadcastEliminationStatus(p.getClientId(), false);
            }
        });
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
            sendCurrentTime(TimerType.ROUND, -1);
        }
    }

    @Override
    protected void sendGameEvent(String msg) {
        clientsInRoom.values().forEach(p -> p.sendGameEvent(msg));
    }

    private void sendToAllClients(Payload payload) {
        clientsInRoom.values().forEach(p -> p.sendToClient(payload));
    }

    private void syncPlayerPoints(ServerThread p) {
        clientsInRoom.values().forEach(other -> {
            if (other != p)
                p.sendPoints(other.getClientId(), other.getPoints());
        });
    }

    @Override
    protected void sendReadyStatus(ServerThread player, boolean isReady) {
        clientsInRoom.values().forEach(p -> p.sendReadyStatus(player.getClientId(), isReady));
    }

    private void sendTurnStatus(ServerThread client, boolean tookTurn) {
        clientsInRoom.values().forEach(p -> p.sendTurnStatus(client.getClientId(), tookTurn));
    }

    protected void handleExtraOptionsToggle(ServerThread player) {
        if (currentPhase != Phase.READY) {
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "You can only toggle options during the ready phase.");
            return;
        }

        // UCID: gb373
        // Date: 07/24/2025
        // Summary: Toggle extra options for the game and only the host can do this.
        boolean isHost = isHost(player);
        if (!isHost) {
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Only the host can toggle extra options.");
            return;
        }

        this.extraOptionsEnabled = !this.extraOptionsEnabled;

        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.EXTRA_OPTIONS_ENABLED);
        payload.setMessage(Boolean.toString(this.extraOptionsEnabled));
        sendToAllClients(payload);

        sendGameEvent("Extra options are now " + (extraOptionsEnabled ? "ENABLED" : "DISABLED"));
    }

    // UCID: gb373
    // Date: 07/24/2025
    // Summary: Battle logic for Rock-Paper-Scissors with extra options.
    private boolean winsAgainst(String a, String b) {
        if (!extraOptionsEnabled) {
            // Original RPS
            return (a.equals("r") && b.equals("s")) || // Rock beats Scissors
                    (a.equals("p") && b.equals("r")) || // Paper beats Rock
                    (a.equals("s") && b.equals("p")); // Scissors beats Paper
        } else {

            if (a.equals("r")) {
                return b.equals("s") || b.equals("f"); // Rock beats Scissors and Fire
            } else if (a.equals("p")) {
                return b.equals("r") || b.equals("w"); // Paper beats Rock and Water
            } else if (a.equals("s")) {
                return b.equals("p") || b.equals("w"); // Scissors beats Paper and Water
            } else if (a.equals("f")) { // Fire
                return b.equals("p") || b.equals("s"); // Fire beats Paper and Scissors
            } else if (a.equals("w")) { // Water
                return b.equals("f") || b.equals("r"); // Water beats Fire and Rock
            }
            return false;
        }
    }

    @Override
    protected void sendCurrentTime(TimerType type, int time) {
        TimerPayload payload = new TimerPayload();
        payload.setPayloadType(PayloadType.TIME);
        payload.setTimerType(type);
        payload.setTime(time);
        sendToAllClients(payload);
    }

    @Override
    protected void syncCurrentPhase(ServerThread p) {
        p.sendCurrentPhase(currentPhase);
    }

    @Override
    protected void syncReadyStatus(ServerThread p) {
        clientsInRoom.values().forEach(other -> {
            if (other != p) {
                p.sendReadyStatus(other.getClientId(), other.isReady(), true);
                p.sendSpectatorStatus(other.getClientId(), other.isSpectator());
            }
        });
    }

    private void syncTurnStatus(ServerThread p) {
        clientsInRoom.values().forEach(other -> {
            if (other != p)
                p.sendTurnStatus(other.getClientId(), other.didTakeTurn(), true);
        });
    }

    private void broadcastEliminationStatus(long clientId, boolean isEliminated) {
        clientsInRoom.values().forEach(client -> {
            client.sendEliminationStatus(clientId, isEliminated);
        });
    }

    private void broadcastSpectatorStatus(long clientId, boolean isSpectator) {
        clientsInRoom.values().forEach(client -> {
            client.sendSpectatorStatus(clientId, isSpectator);
        });
    }

    private boolean isHost(ServerThread player) {
        return clientsInRoom.values().stream().findFirst().map(p -> p.getClientId() == player.getClientId())
                .orElse(false);
    }

    private boolean choiceCooldownEnabled = false;

    // UCID: gb373
    // Date: 07/28/2025
    // Summary: Toggle choice cooldown for the game; only the host can do this.
    protected void handleChoiceCooldownToggle(ServerThread player) {
        if (currentPhase != Phase.READY) {
            player.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You can only toggle choice cooldown during the ready phase.");
            return;
        }

        boolean isHost = isHost(player);
        if (!isHost) {
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Only the host can toggle choice cooldown.");
            return;
        }

        this.choiceCooldownEnabled = !this.choiceCooldownEnabled;

        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.CHOICE_COOLDOWN_TOGGLE);
        payload.setMessage(Boolean.toString(this.choiceCooldownEnabled));
        sendToAllClients(payload);

        sendGameEvent("Choice cooldown is now " + (choiceCooldownEnabled ? "ENABLED" : "DISABLED"));
    }

    public boolean isChoiceCooldownEnabled() {
        return choiceCooldownEnabled;
    }

    // UCID: gb373
    // Date: 07/28/2025
    // Summary: Handles the away toggle for a player.
    protected void handleAwayToggle(ServerThread player, boolean newAway) {
        player.setAway(newAway);

        Payload p = new Payload();
        p.setPayloadType(PayloadType.AWAY_UPDATE);
        p.setClientId(player.getClientId());
        p.setMessage(String.valueOf(newAway));
        sendToAllClients(p);
    }

}
