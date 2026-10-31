import ui.MainFrame;
import ui.Theme;

import javax.swing.*;

/** Entry point: starts the Swing window on the Event Dispatch Thread. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.apply();
            new MainFrame().setVisible(true);
        });
    }
}
