package Project.Server;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import Project.Common.TextFX.Color;
import Project.Common.TimerPayload;
import Project.Common.TimerType;
import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.Phase;
import Project.Common.PointsPayload;
import Project.Common.ReadyPayload;
import Project.Common.RoomAction;
import Project.Common.RoomResultPayload;
import Project.Common.TextFX;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete; // callback to inform when this object is ready

    /**
     * A wrapper method so we don't need to keep typing out the long/complex sysout
     * line inside
     * 
     * @param message
     */
    @Override
    protected void info(String message) {
        LoggerUtil.INSTANCE
                .info(TextFX.colorize(String.format("Thread[%s]: %s", this.getClientId(), message), Color.CYAN));
    }

    /**
     * Wraps the Socket connection and takes a Server reference and a callback
     * 
     * @param myClient
     * @param server
     * @param onInitializationComplete method to inform listener that this object is
     *                                 ready
     */
    protected ServerThread(Socket myClient, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        // this.clientId = this.threadId(); // An id associated with the thread
        // instance, used as a temporary identifier
        this.onInitializationComplete = onInitializationComplete;

    }

    // Start Send*() Methods
    public boolean sendResetTurnStatus() {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.RESET_TURN);
        return sendToClient(rp);
    }

    // UCID: gb373
    // Date: 07/10/2025
    // Summary: Sends the turn status to the client, indicating whether they took
    // their turn or not.
    // If quiet is true, it uses SYNC_TURN to silently update the status without
    // showing output on the client side.
    public boolean sendTurnStatus(long clientId, boolean didTakeTurn) {
        return sendTurnStatus(clientId, didTakeTurn, false);
    }

    public boolean sendTurnStatus(long clientId, boolean didTakeTurn, boolean quiet) {
        // NOTE for now using ReadyPayload as it has the necessary properties
        // An actual turn may include other data for your project
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(quiet ? PayloadType.SYNC_TURN : PayloadType.TURN);
        rp.setClientId(clientId);
        rp.setReady(didTakeTurn);
        return sendToClient(rp);
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Sends a game event message to the client, which can be used for
    // various game-related notifications.
    public boolean sendGameEvent(String message) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.GAME_EVENT); // Make sure this enum exists in PayloadType
        payload.setMessage(message);
        return sendToClient(payload);
    }

    public boolean sendCurrentTime(TimerType timerType, int time) {
        TimerPayload payload = new TimerPayload();
        payload.setTimerType(timerType);
        payload.setTime(time);
        return sendToClient(payload);
    }

    public boolean sendCurrentPhase(Phase phase) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.PHASE);
        p.setMessage(phase.name());
        return sendToClient(p);
    }

    public boolean sendResetReady() {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.RESET_READY);
        return sendToClient(rp);
    }

    public boolean sendReadyStatus(long clientId, boolean isReady) {
        return sendReadyStatus(clientId, isReady, false);
    }

    /**
     * Sync ready status of client id
     * 
     * @param clientId who
     * @param isReady  ready or not
     * @param quiet    silently mark ready
     * @return
     */
    public boolean sendReadyStatus(long clientId, boolean isReady, boolean quiet) {
        ReadyPayload rp = new ReadyPayload();
        rp.setClientId(clientId);
        rp.setReady(isReady);
        if (quiet) {
            rp.setPayloadType(PayloadType.SYNC_READY);
        }
        return sendToClient(rp);
    }

    public boolean sendRooms(List<String> rooms) {
        RoomResultPayload rrp = new RoomResultPayload();
        rrp.setRooms(rooms);
        return sendToClient(rrp);
    }

    protected boolean sendDisconnect(long clientId) {
        Payload payload = new Payload();
        payload.setClientId(clientId);
        payload.setPayloadType(PayloadType.DISCONNECT);
        return sendToClient(payload);
    }

    protected boolean sendResetUserList() {
        return sendClientInfo(Constants.DEFAULT_CLIENT_ID, null, RoomAction.JOIN);
    }

    // UCID: gb373
    // Date: 07/22/2025
    // Summary: Sends points to the client, which can be used for scoring or other
    // purposes.
    public boolean sendPoints(long clientId, int points) {
        PointsPayload pp = new PointsPayload();
        pp.setPayloadType(PayloadType.POINTS);
        pp.setClientId(clientId);
        pp.setPoints(points);
        return sendToClient(pp);
    }

    /**
     * Syncs Client Info (id, name, join status) to the client
     * 
     * @param clientId   use -1 for reset/clear
     * @param clientName
     * @param action     RoomAction of Join or Leave
     * @return true for successful send
     */
    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action) {
        return sendClientInfo(clientId, clientName, action, false);
    }

    /**
     * Syncs Client Info (id, name, join status) to the client
     * 
     * @param clientId   use -1 for reset/clear
     * @param clientName
     * @param action     RoomAction of Join or Leave
     * @param isSync     True is used to not show output on the client side (silent
     *                   sync)
     * @return true for successful send
     */
    protected boolean sendClientInfo(long clientId, String clientName, RoomAction action, boolean isSync) {
        ConnectionPayload payload = new ConnectionPayload();
        switch (action) {
            case JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            default:
                break;
        }
        if (isSync) {
            payload.setPayloadType(PayloadType.SYNC_CLIENT);
        }
        payload.setClientId(clientId);
        payload.setClientName(clientName);
        return sendToClient(payload);
    }

    /**
     * Sends this client's id to the client.
     * This will be a successfully connection handshake
     * 
     * @return true for successful send
     */
    protected boolean sendClientId() {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setPayloadType(PayloadType.CLIENT_ID);
        payload.setClientId(getClientId());
        payload.setClientName(getClientName());// Can be used as a Server-side override of username (i.e., profanity
                                               // filter)
        return sendToClient(payload);
    }

    /**
     * Sends a message to the client
     * 
     * @param clientId who it's from
     * @param message
     * @return true for successful send
     */
    protected boolean sendMessage(long clientId, String message) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.MESSAGE);
        payload.setMessage(message);
        payload.setClientId(clientId);
        return sendToClient(payload);
    }

    // End Send*() Methods
    @Override
    protected void processPayload(Payload incoming) {
        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT:
                setClientName(((ConnectionPayload) incoming).getClientName().trim());
                break;
            case DISCONNECT:
                currentRoom.handleDisconnect(this);
                break;
            case MESSAGE:
                currentRoom.handleMessage(this, incoming.getMessage());
                break;
            case REVERSE:
                currentRoom.handleReverseText(this, incoming.getMessage());
                break;
            case ROOM_CREATE:
                currentRoom.handleCreateRoom(this, incoming.getMessage());
                break;
            case ROOM_JOIN:
                currentRoom.handleJoinRoom(this, incoming.getMessage());
                break;
            case ROOM_LEAVE:
                currentRoom.handleJoinRoom(this, Room.LOBBY);
                break;
            case ROOM_LIST:
                currentRoom.handleListRooms(this, incoming.getMessage());
                break;
            case READY:
                try {
                    ((GameRoom) currentRoom).handleReady(this);
                } catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
                }
                break;
            // UCID: gb373
            // Date: 07/09/2025
            // Summary: Handles the player's turn action in the game.
            case TURN:
                try {
                    ((GameRoom) currentRoom).handleTurnAction(this, incoming.getMessage());
                } catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do a turn");
                }
                break;
            case TIME:
                TimerPayload timerPayload = (TimerPayload) incoming;
                // Update UI timer display, e.g.:
                System.out.println("Timer update: " + timerPayload.getTime() + " seconds remaining.");
                break;
            case POINTS:
                PointsPayload pointsPayload = (PointsPayload) incoming;
                // Update player points UI:
                System.out.println(
                        "Player " + pointsPayload.getClientId() + " has " + pointsPayload.getPoints() + " points.");
                break;
            case RESET_TURN:
            case SYNC_TURN:
                ReadyPayload turnPayload = (ReadyPayload) incoming;
                // Update turn status in UI, e.g.:
                System.out.println("Player " + turnPayload.getClientId() + " turn status: " + turnPayload.isReady());
                break;
            case RESET_READY:
            case SYNC_READY:
                ReadyPayload readyPayload = (ReadyPayload) incoming;
                // Update ready status in UI, e.g.:
                System.out.println("Player " + readyPayload.getClientId() + " ready status: " + readyPayload.isReady());
                break;
            case GAME_EVENT:
                Payload gameEventPayload = (Payload) incoming;
                // Handle game event, e.g.:
                System.out.println("Game event: " + gameEventPayload.getMessage());
                break;
            case CLIENT_ID:
                // This is the initial connection payload, so we can set the client ID
                setClientId(incoming.getClientId());
                // Send the client ID back to the client
                sendClientId();
                // Notify that initialization is complete
                onInitialized();
                break;
            case EXTRA_OPTIONS_TOGGLE:
                try {
                    ((GameRoom) currentRoom).handleExtraOptionsToggle(this);
                } catch (Exception e) {
                    sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to toggle extra options.");
                }
                break;

            default:
                LoggerUtil.INSTANCE.warning(
                        TextFX.colorize("Unknown payload type received: " + incoming.getPayloadType(), Color.RED));
                break;
        }
    }

    // limited user data exposer
    protected boolean isReady() {
        return this.user.isReady();
    }

    protected void setReady(boolean isReady) {
        this.user.setReady(isReady);
    }

    protected boolean didTakeTurn() {
        return this.user.didTakeTurn();
    }

    protected void setTookTurn(boolean tookTurn) {
        this.user.setTookTurn(tookTurn);
    }

    @Override
    protected void onInitialized() {
        // once receiving the desired client name the object is ready
        onInitializationComplete.accept(this);
    }

    public int getPoints() {
        return user.getPoints();
    }

    private String choice; // Player's current choice (e.g., "r", "p", "s")

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    private boolean eliminated = false;

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public void setPoints(int points) {
        user.setPoints(points);
    }

    public void changePoints(int delta) {
        int updatedPoints = getPoints() + delta;
        setPoints(updatedPoints);
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Sends the pending pick status to the client, indicating whether the
    // player is currently pending a pick action.
    public boolean sendPendingStatus(long clientId, boolean isPending) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.PENDING_PICK);
        payload.setClientId(clientId);
        payload.setMessage(Boolean.toString(isPending));
        return sendToClient(payload);
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Sends the elimination status to the client, indicating whether the
    // player has been eliminated.
    public boolean sendEliminationStatus(long clientId, boolean isEliminated) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.ELIMINATED); // Define this enum
        payload.setClientId(clientId);
        payload.setMessage(Boolean.toString(isEliminated));
        return sendToClient(payload);
    }

}