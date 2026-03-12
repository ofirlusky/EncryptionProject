import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // יוצרים את המשחק
            PicariaGame game = new PicariaGame();

            // מריצים את ה-GUI
            PicariaGUI gui = new PicariaGUI(game);
            gui.setVisible(true);
        });
    }
}