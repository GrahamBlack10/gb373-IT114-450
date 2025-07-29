package Project.Client.Interfaces;

public interface IPointsEvent extends IGameEvents {
    /**
     * Receives the current phase
     * 
     * @param phase
     */
    void onPointsUpdate(long clientId, int points);
    
    // Added to support pending pick status
    void onPendingPick(long clientId, boolean isPending);
    // Added to supprt eliminated status
    void onEliminationStatus(long clientId, boolean isEliminated);
}
