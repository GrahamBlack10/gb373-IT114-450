package Project.Client.Views;

import javax.swing.JPanel;
import javax.swing.JButton;
import Project.Client.Client;
import java.io.IOException;
import Project.Common.Phase;

//UCID: gb373
//Date: 07/23/2025
//Summary: PlayView allows users to play the game by selecting Rock, Paper, or Scissors.
// It sends the user's choice to the server and updates the UI based on the game phase.
public class PlayView extends JPanel {
    private final JButton rockButton = new JButton("Rock");
    private final JButton paperButton = new JButton("Paper");
    private final JButton scissorsButton = new JButton("Scissors");
    private final JButton fireButton = new JButton("Fire");
    private final JButton waterButton = new JButton("Water");

    private Phase currentPhase;

    private boolean extraOptionsEnabled = false;

    public PlayView(String name) {
        this.add(rockButton);
        this.add(paperButton);
        this.add(scissorsButton);
        this.add(fireButton);
        this.add(waterButton);

        rockButton.addActionListener(e -> sendPick("r"));
        paperButton.addActionListener(e -> sendPick("p"));
        scissorsButton.addActionListener(e -> sendPick("s"));
        fireButton.addActionListener(e -> sendPick("f"));
        waterButton.addActionListener(e -> sendPick("w"));

        setButtonsVisible(false);
    }

    public void setExtraOptionsEnabled(boolean enabled) {
        this.extraOptionsEnabled = enabled;
        System.out.println("setExtraOptionsEnabled called with: " + enabled);

        if (currentPhase == Phase.IN_PROGRESS) {
            setButtonsVisible(true);

            fireButton.setEnabled(enabled);
            waterButton.setEnabled(enabled);
        } else {
            fireButton.setVisible(false);
            waterButton.setVisible(false);
        }
        revalidate();
        repaint();
    }

    private void sendPick(String choice) {
        try {
            Client.INSTANCE.sendDoTurn(choice);
            highlightSelectedButton(choice);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // UCID: gb373
    // Date: 07/23/2025
    // Summary: Highlights the selected button and disables others to indicate the user's choice.
    // This helps prevent multiple selections during the turn.
    // Has Extra Options: Fire and Water
    private void highlightSelectedButton(String choice) {
        rockButton.setEnabled(!"r".equals(choice));
        paperButton.setEnabled(!"p".equals(choice));
        scissorsButton.setEnabled(!"s".equals(choice));
        fireButton.setEnabled(!"f".equals(choice));
        waterButton.setEnabled(!"w".equals(choice));
    }

    public void changePhase(Phase phase) {
        this.currentPhase = phase;
        this.extraOptionsEnabled = Client.INSTANCE.isExtraOptionsEnabled();

        System.out.println("changePhase called with: " + phase);

        if (phase == Phase.IN_PROGRESS) {
            setButtonsVisible(true);
            rockButton.setEnabled(true);
            paperButton.setEnabled(true);
            scissorsButton.setEnabled(true);
            fireButton.setEnabled(true);
            waterButton.setEnabled(true);
        } 
    }

    private void setButtonsVisible(boolean visible) {
        System.out.println(
                "setButtonsVisible called: visible=" + visible + ", extraOptionsEnabled=" + extraOptionsEnabled);
        rockButton.setVisible(visible);
        paperButton.setVisible(visible);
        scissorsButton.setVisible(visible);
        fireButton.setVisible(visible && extraOptionsEnabled);
        waterButton.setVisible(visible && extraOptionsEnabled);
        revalidate();
        repaint();
    }

    public void hideButtonsAfterPick() {
        setButtonsVisible(false);
    }

    public void setHost(boolean hostStatus) {
    }

}
