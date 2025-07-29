package Project.Client.Views;

import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;
import Project.Client.Client;
import Project.Common.Payload;
import Project.Common.PayloadType;

public class ReadyView extends JPanel {
    public ReadyView() {
        // Ready button
        JButton readyButton = new JButton("Ready");
        readyButton.addActionListener(e -> {
            try {
                Client.INSTANCE.sendReady();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.add(readyButton);

        // UCID: gb373
        // Date: 07/24/2025
        // Summary: This button allows the user to toggle extra options in the game.
        // Extra Options toggle button
        JButton extraButton = new JButton("Toggle Extra Options");
        extraButton.addActionListener(e -> {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.EXTRA_OPTIONS_TOGGLE); // Must be added to PayloadType
            p.setMessage("true"); // Simple toggle signal
            try {
                Client.INSTANCE.sendPayload(p); // Send to server
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        this.add(extraButton);

        // UCID: gb373
        // Date: 07/28/2025
        // Summary: This button allows the user to toggle cooldown options in the game.
        JButton cooldownButton = new JButton("Toggle Cooldown");
        cooldownButton.addActionListener(e -> {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.CHOICE_COOLDOWN_TOGGLE); 
            p.setMessage("true"); 
            try {
                Client.INSTANCE.sendPayload(p);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        this.add(cooldownButton);
    }
}
