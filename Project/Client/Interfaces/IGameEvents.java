package Project.Client.Interfaces;

/**
 * Base-class for game specific events (used for organizing)
 */
public interface IGameEvents extends IClientEvents {
    void onAwayStatusChange(long clientId, boolean isAway);
    void onAwayStatusToggle(boolean isAway);
    void onSpectatorStatusChange(long clientId, boolean isSpectator);
}