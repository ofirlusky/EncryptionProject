package gui;

import game.PicariaGame;
import game.PieceValue;
import game.PlayerID;
import utils.Node;

import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel {

    private PicariaGame game;
    private JLabel statusLabel;

    // כפתור שמופיע רק כשיש מנצח
    private JButton continueButton;
    private boolean cryptoLaunched = false;

    public BoardPanel(PicariaGame game, JLabel statusLabel) {
        this.game = game;
        this.statusLabel = statusLabel;
        setBackground(Color.black);
        setLayout(null);   // מיקום ידני לכפתור

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });

        // כפתור "המשך לקריפטו" — מוסתר עד ניצחון
        continueButton = new JButton("CONTINUE TO CRYPTO →");
        continueButton.setFont(new Font("Arial", Font.BOLD, 18));
        continueButton.setBackground(new Color(46, 160, 67));
        continueButton.setForeground(Color.BLACK);
        continueButton.setFocusPainted(false);
        continueButton.setBounds(160, 600, 300, 50);
        continueButton.setVisible(false);
        continueButton.addActionListener(e -> launchCrypto());
        add(continueButton);
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        if (game.isGameOver()) return;   // אחרי ניצחון לא מקבלים קליקים

        int padding = 100;
        int scale = 100;
        for (int i = 0; i < PicariaGame.TOTAL_NODES; i++) {
            int nodeX = padding + (PicariaGame.COORDS[i][0] * scale);
            int nodeY = padding + (PicariaGame.COORDS[i][1] * scale);

            double distance = Math.sqrt(Math.pow(mouseX - nodeX, 2) + Math.pow(mouseY - nodeY, 2));

            if (distance < 30) {
                System.out.println("Clicked on node: " + i);
                game.playTurn(i);
                updateStatusMessage();
                repaint();
                break;
            }
        }
    }

    /**
     * סוגר את חלון המשחק ופותח את חלון הקריפטו.
     */
    private void launchCrypto() {
        if (cryptoLaunched) return;
        cryptoLaunched = true;

        // מאתר את חלון המשחק (ה-JFrame ההורה) וסוגר אותו
        Window gameWindow = SwingUtilities.getWindowAncestor(this);

        // פותח את חלון הקריפטו עם נתוני המשחק
        new CryptoResultGUI(game.getBoard(), game.getGameHistory());

        if (gameWindow != null) {
            gameWindow.dispose();   // סוגר את חלון המשחק
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int padding = 100;
        int scale = 100;
        int nodeSize = 40;
        int pieceSize = 30;
        int nodeOffset = nodeSize / 2;
        int pieceOffset = pieceSize / 2;

        g.setColor(Color.WHITE);
        for (int i = 0; i < PicariaGame.TOTAL_NODES; i++) {
            int x1 = padding + (PicariaGame.COORDS[i][0] * scale);
            int y1 = padding + (PicariaGame.COORDS[i][1] * scale);

            for (int neighborIndex : PicariaGame.neighbors[i]) {
                int x2 = padding + (PicariaGame.COORDS[neighborIndex][0] * scale);
                int y2 = padding + (PicariaGame.COORDS[neighborIndex][1] * scale);
                g.drawLine(x1, y1, x2, y2);
            }
        }
        for (int i = 0; i < PicariaGame.TOTAL_NODES; i++) {
            int screenX = padding + (PicariaGame.COORDS[i][0] * scale);
            int screenY = padding + (PicariaGame.COORDS[i][1] * scale);

            g.setColor(Color.WHITE);
            g.fillOval(screenX - nodeOffset, screenY - nodeOffset, nodeSize, nodeSize);
            Node selected = game.getSelectedNode();
            if (selected != null && selected.getIDofNode() == i) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawOval(screenX - nodeOffset, screenY - nodeOffset, nodeSize, nodeSize);
                g2d.setStroke(new BasicStroke(1));
            }

            PieceValue status = game.getBoard()[i].getCurrentPieceValue();
            if (status == PieceValue.OCCUPIED_P1) {
                g.setColor(Color.BLUE);
                g.fillOval(screenX - pieceOffset, screenY - pieceOffset, pieceSize, pieceSize);
            } else if (status == PieceValue.OCCUPIED_P2) {
                g.setColor(Color.RED);
                g.fillOval(screenX - pieceOffset, screenY - pieceOffset, pieceSize, pieceSize);
            }
        }
    }

    private void updateStatusMessage() {
        if (game.isGameOver()) {
            String winner = (game.getCurrentTurn() == PlayerID.PLAYER_ONE)
                    ? "Player 1 (Blue)" : "Player 2 (Red)";
            statusLabel.setText("GAME OVER - " + winner + " wins!");
            statusLabel.setForeground(Color.YELLOW);

            // מציג את כפתור ההמשך לקריפטו
            continueButton.setVisible(true);
            repaint();
            return;
        }

        String playerColor = (game.getCurrentTurn() == PlayerID.PLAYER_ONE)
                ? "Player 1 (Blue)" : "Player 2 (Red)";
        String phaseText;
        if (game.getNumberOfpiecesPlaced() < 6) {
            phaseText = "Drop Phase (Place a piece)";
        } else {
            phaseText = "Move Phase (Select & Move)";
        }
        statusLabel.setText(playerColor + " Turn - " + phaseText);

        if (game.getCurrentTurn() == PlayerID.PLAYER_ONE) {
            statusLabel.setForeground(Color.BLUE);
        } else {
            statusLabel.setForeground(Color.RED);
        }
    }
}