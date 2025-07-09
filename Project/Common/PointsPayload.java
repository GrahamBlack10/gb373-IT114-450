// UCID: gb373 Date: 07/09/2025 Summaery: PointsPayload class added to handle points data transfer.
package Project.Common;

public class PointsPayload extends Payload {
    private int points;

    public PointsPayload() {
        // set payload type to POINTS_UPDATE or similar
        this.setPayloadType(PayloadType.POINTS_UPDATE);
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return String.format("PointsPayload { points: %d}",
                points);
    }
}
