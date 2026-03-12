import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PicariaGUI extends JFrame {

    private PicariaGame game;
    private final int NODE_RADIUS = 20;
    private int selectedNode = -1;

    // הרכיב החדש: שלט הטקסט שיופיע למעלה
    private JLabel statusLabel;

    public PicariaGUI(PicariaGame game) {
        this.game = game;

        setTitle("Picaria Game - Fixed 3x3 Board");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // מגדירים את פריסת החלון (BorderLayout מאפשר לשים דברים ב"צפון", "דרום", "מרכז" וכו')
        setLayout(new BorderLayout());

        // --- יצירת שלט הסטטוס העליון ---
        statusLabel = new JLabel("", SwingConstants.CENTER); // ממורכז לאמצע
        statusLabel.setFont(new Font("Arial", Font.BOLD, 24)); // גופן גדול ובולט
        statusLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0)); // קצת רווח מלמעלה ומלמטה
        updateStatusLabel(); // קריאה ראשונה כדי לעדכן את הטקסט לתחילת המשחק
        add(statusLabel, BorderLayout.NORTH); // מדביקים אותו בחלק העליון של החלון ("צפון")

        // --- יצירת משטח הציור ---
        BoardPanel panel = new BoardPanel();
        panel.setPreferredSize(new Dimension(650, 650));
        panel.setBackground(Color.WHITE);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();

                int maxCoord = 4;
                int padding = 80;
                int minDimension = Math.min(panel.getWidth(), panel.getHeight());
                int spacing = (minDimension - (2 * padding)) / maxCoord;

                int boardSize = maxCoord * spacing;
                int startX = (panel.getWidth() - boardSize) / 2;
                int startY = (panel.getHeight() - boardSize) / 2;

                for (int i = 0; i < game.getTotalNodes(); i++) {
                    Point p = game.getNodeCoord(i);
                    int nodeX = startX + p.x * spacing;
                    int nodeY = startY + p.y * spacing;

                    double distance = Math.sqrt(Math.pow(mouseX - nodeX, 2) + Math.pow(mouseY - nodeY, 2));

                    if (distance <= NODE_RADIUS + 5) {

                        // שלב 1: שלב ההנחה (Drop Phase)
                        if (game.getPiecesPlaced() < 6) {
                            if (game.placePiece(i)) {
                                updateStatusLabel();
                                panel.repaint();
                                checkForWin();
                            }
                        }
                        // שלב 2: שלב התזוזה (Move Phase)
                        else {
                            // אם עדיין לא בחרנו חייל להזיז (לחיצה ראשונה)
                            if (selectedNode == -1) {
                                // בודקים אם השחקן לחץ על חייל שלו
                                if (game.getBoard()[i] == game.getCurrentPlayer()) {
                                    selectedNode = i; // מסמנים את החייל
                                    System.out.println("Selected node: " + i);
                                    panel.repaint();
                                }
                            }
                            // אם כבר נבחר חייל (לחיצה שנייה על היעד)
                            else {
                                // אם השחקן התחרט ולחץ על חייל אחר שלו - נשנה את הבחירה
                                if (game.getBoard()[i] == game.getCurrentPlayer()) {
                                    selectedNode = i;
                                    panel.repaint();
                                }
                                // ניסיון תזוזה ליעד
                                else {
                                    if (game.movePiece(selectedNode, i)) {
                                        selectedNode = -1; // איפוס בחירה אחרי הצלחה
                                        updateStatusLabel();
                                        panel.repaint();
                                        checkForWin();
                                    } else {
                                        // תנועה לא חוקית - נבטל את הבחירה כדי לנסות שוב
                                        selectedNode = -1;
                                        panel.repaint();
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        });

        // מדביקים את הלוח במרכז החלון
        add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    // --- הפונקציה שמעדכנת את הטקסט והצבע לפי מצב המשחק ---
    private void updateStatusLabel() {
        String playerText = (game.getCurrentPlayer() == 1) ? "Player 1 (Blue)" : "Player 2 (Yellow)";
        String phaseText = (game.getPiecesPlaced() < 6) ? "Drop Phase: Place a piece" : "Move Phase: Select and move";

        statusLabel.setText(playerText + " | " + phaseText);

        // צובע את הטקסט לפי השחקן כדי שיהיה קל לזהות במבט חטוף
        if (game.getCurrentPlayer() == 1) {
            statusLabel.setForeground(Color.BLUE);
        } else {
            // צבע חרדל קצת יותר כהה מצהוב רגיל, כדי שיהיה אפשר לקרוא את זה על רקע לבן
            statusLabel.setForeground(new Color(204, 153, 0));
        }
    }

    private void checkForWin() {
        int winner = game.checkWinner();
        if (winner != 0) {
            String winnerName = (winner == 1) ? "Player 1 (Blue)" : "Player 2 (Yellow)";

            // עדכון הכותרת למעלה
            statusLabel.setText(winnerName + " WINS THE GAME!");
            statusLabel.setForeground(Color.RED);

            // ציור אחרון כדי לראות את המהלך המנצח
            repaint();

            // חלון קופץ שמכריז על הניצחון
            JOptionPane.showMessageDialog(this,
                    winnerName + " has won the game!",
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);

            // סגירת המשחק (או שאפשר פשוט לצאת מהתוכנית)
            System.exit(0);
        }
    }

    private class BoardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int maxCoord = 4;
            int padding = 80;

            int minDimension = Math.min(getWidth(), getHeight());
            int spacing = (minDimension - (2 * padding)) / maxCoord;

            int boardSize = maxCoord * spacing;
            int startX = (getWidth() - boardSize) / 2;
            int startY = (getHeight() - boardSize) / 2;

            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(3));

            for (int i = 0; i < game.getTotalNodes(); i++) {
                Point p1 = game.getNodeCoord(i);
                int[] neighbors = game.getNeighborsOf(i);

                for (int neighbor : neighbors) {
                    if (neighbor > i) {
                        Point p2 = game.getNodeCoord(neighbor);
                        int x1 = startX + p1.x * spacing;
                        int y1 = startY + p1.y * spacing;
                        int x2 = startX + p2.x * spacing;
                        int y2 = startY + p2.y * spacing;
                        g2d.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            int[] board = game.getBoard();
            for (int i = 0; i < game.getTotalNodes(); i++) {
                Point p = game.getNodeCoord(i);
                int cx = startX + p.x * spacing;
                int cy = startY + p.y * spacing;
                int owner = board[i];

                if (owner == 0) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(cx - NODE_RADIUS, cy - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval(cx - NODE_RADIUS, cy - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
                } else if (owner == 1) {
                    g2d.setColor(Color.BLUE);
                    g2d.fillOval(cx - NODE_RADIUS, cy - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
                } else if (owner == 2) {
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(cx - NODE_RADIUS, cy - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
                }

                if (i == selectedNode) {
                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(4));
                    g2d.drawOval(cx - NODE_RADIUS - 6, cy - NODE_RADIUS - 6, (2 * NODE_RADIUS) + 12, (2 * NODE_RADIUS) + 12);
                }
            }
        }
    }
}