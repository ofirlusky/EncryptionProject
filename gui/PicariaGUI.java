package gui;

import game.PicariaGame;

import javax.swing.*;
import java.awt.*;

public class PicariaGUI extends JFrame {

    private PicariaGame game;
    private JLabel statusLabel;

    public PicariaGUI() {
        setTitle("The Game Of The Lusexy");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(620, 700);
        setLayout(new BorderLayout());


        ImageIcon image = new ImageIcon("gui/image.png");
        setIconImage(image.getImage());


        game = new PicariaGame();

        statusLabel = new JLabel("game.Player 1 (Blue) - Drop Phase", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 22));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        BoardPanel boardPanel = new BoardPanel(game, statusLabel);
        add(statusLabel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);


        setLocationRelativeTo(null);
        setVisible(true);
    }



}




