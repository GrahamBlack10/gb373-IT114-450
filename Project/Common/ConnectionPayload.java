package Project.Common;

public class ConnectionPayload extends Payload {
    private String clientName;
    private boolean wantsSpectator;

    /**
     * @return the clientName
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * @param clientName the clientName to set
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public String toString() {
        return super.toString() +
                String.format(" ClientName: [%s]",
                        getClientName());
    }

    public void setWantsSpectator(boolean wantsSpectator) {
        this.wantsSpectator = wantsSpectator;
    }

    public boolean getWantsSpectator() {
        return wantsSpectator;
    }
}
