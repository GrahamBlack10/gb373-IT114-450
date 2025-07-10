package Project.Common;

import java.io.Serializable;

public class Payload implements Serializable {
    //Ucid: gb373
    //Date: 07/09/2025
    //Summary: Base class for payloads used in client-server communication.
    private static final long serialVersionUID = 1L;

    private PayloadType payloadType;
    private long clientId;
    private String message;

    /**
     * @return the payloadType
     */
    public PayloadType getPayloadType() {
        return payloadType;
    }

    /**
     * @param payloadType the payloadType to set
     */
    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

    /**
     * @return the clientId
     */
    public long getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("Payload[%s] Client Id [%d] Message: [%s]",
                getPayloadType(), getClientId(), getMessage());
    }
}