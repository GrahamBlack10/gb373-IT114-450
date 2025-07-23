package Project.Client.Views;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.io.IOException;
import Project.Client.Client;
import Project.Common.Phase;

//UCID: gb373
//Date: 07/23/2025
//Summary: PlayView allows users to play the game by selecting Rock, Paper, or Scissors.
// It sends the user's choice to the server and updates the UI based on the game phase.
public class PlayView extends JPanel {
    private final JButton rockButton = new JButton("Rock");
    private final JButton paperButton = new JButton("Paper");
    private final JButton scissorsButton = new JButton("Scissors");

    public PlayView(String name) {
        this.setName(name);

        // Add buttons to the panel
        this.add(rockButton);
        this.add(paperButton);
        this.add(scissorsButton);

        // Set what happens when each button is clicked
        rockButton.addActionListener(e -> sendPick("r"));
        paperButton.addActionListener(e -> sendPick("p"));
        scissorsButton.addActionListener(e -> sendPick("s"));

        // Hide buttons at first
        setButtonsVisible(false);
    }

    // Method to send the choice to the server and highlight the selection
    private void sendPick(String choice) {
        try {
            Client.INSTANCE.sendDoTurn(choice); // Sends to server
            highlightSelectedButton(choice);
            setButtonsVisible(false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void highlightSelectedButton(String choice) {
        rockButton.setEnabled(!"r".equals(choice));
        paperButton.setEnabled(!"p".equals(choice));
        scissorsButton.setEnabled(!"s".equals(choice));
    }

    public void changePhase(Phase phase) {
        if (phase == Phase.IN_PROGRESS) {
            setButtonsVisible(true);
            // Reset buttons to enabled for a new round
            rockButton.setEnabled(true);
            paperButton.setEnabled(true);
            scissorsButton.setEnabled(true);
        } else {
            setButtonsVisible(false);
        }
    }

    private void setButtonsVisible(boolean visible) {
        rockButton.setVisible(visible);
        paperButton.setVisible(visible);
        scissorsButton.setVisible(visible);
    }
}
