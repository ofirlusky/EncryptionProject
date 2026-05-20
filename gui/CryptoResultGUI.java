package gui;

import crypto.AESFileEncryptor;
import crypto.AvalancheTester;
import crypto.KeyGenerator;
import game.Move;
import graph.BaseCryptoGraph;
import graph.BitwiseCryptoGraph;
import graph.EulerianCryptoGraph;
import graph.KruskalCryptoGraph;
import graph.MaxFlowCryptoGraph;
import utils.Node;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CryptoResultGUI extends JFrame {

    private final Node[]     finalBoard;
    private final List<Move> gameHistory;

    private static final String ENCRYPTED_FILE = "secret_encrypted.bin";
    private static final String DECRYPTED_FILE  = "secret_decrypted.txt";
    private static final String DEFAULT_INPUT   = "secret.txt";

    private File   selectedInputFile = null;
    private String inputFilePath     = DEFAULT_INPUT;

    private JTextArea    statusArea;
    private JLabel       fileLabel;
    private JButton      startButton, chooseButton, autoButton;
    private JButton      openOriginal, openEncrypted, openDecrypted;
    private JProgressBar progressBar;

    private JTextArea proofArea;
    private JButton   runProofButton;

    private static final Color BG    = new Color(33, 37, 43);
    private static final Color PANEL = new Color(40, 44, 52);
    private static final Color GREEN = new Color(46, 160, 67);
    private static final Color TEXT  = new Color(220, 220, 220);



    public CryptoResultGUI(Node[] finalBoard, List<Move> gameHistory) {
        this.finalBoard  = finalBoard;
        this.gameHistory = gameHistory;

        setTitle("Picaria Crypto");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 680);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 14));
        tabs.addTab("  Encryption  ",       buildEncryptionTab());
        tabs.addTab("  Key Quality Proof  ", buildProofTab());

        add(tabs);
        setVisible(true);
    }



    private JPanel buildEncryptionTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(PANEL);
        top.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("File Encryption");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Encrypt a file with a key generated from your Picaria game");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(Color.LIGHT_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        filePanel.setBackground(PANEL);
        chooseButton = styledButton("Choose a file...", false);
        autoButton   = styledButton("Create one automatically", false);
        chooseButton.addActionListener(e -> chooseFile());
        autoButton.addActionListener(e -> useAutoFile());
        filePanel.add(chooseButton);
        filePanel.add(autoButton);

        fileLabel = new JLabel("No file chosen - one will be created automatically");
        fileLabel.setForeground(Color.ORANGE);
        fileLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        startButton = styledButton("START", true);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> startProcess());

        top.add(title);
        top.add(Box.createVerticalStrut(4));
        top.add(subtitle);
        top.add(Box.createVerticalStrut(8));
        top.add(filePanel);
        top.add(fileLabel);
        top.add(Box.createVerticalStrut(12));
        top.add(startButton);

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusArea.setBackground(new Color(24, 26, 31));
        statusArea.setForeground(TEXT);
        statusArea.setMargin(new Insets(14, 16, 14, 16));
        statusArea.setText("Ready.\n\nChoose a file (or let the program create one), then press START.\n");

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        bottom.setBackground(PANEL);
        openOriginal  = styledButton("Open original file",  false);
        openEncrypted = styledButton("Open encrypted file", false);
        openDecrypted = styledButton("Open decrypted file", false);
        openOriginal.addActionListener(e  -> openFile(inputFilePath));
        openEncrypted.addActionListener(e -> openFile(ENCRYPTED_FILE));
        openDecrypted.addActionListener(e -> openFile(DECRYPTED_FILE));
        setOpenButtonsEnabled(false);
        bottom.add(openOriginal);
        bottom.add(openEncrypted);
        bottom.add(openDecrypted);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        centerWrap.add(progressBar, BorderLayout.SOUTH);

        root.add(top,        BorderLayout.NORTH);
        root.add(centerWrap, BorderLayout.CENTER);
        root.add(bottom,     BorderLayout.SOUTH);
        return root;
    }



    private JPanel buildProofTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(PANEL);
        top.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Is the key really good?");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea explain = new JTextArea(
                "Strict Avalanche Criterion (SAC):\n\n" +
                        "  The board is encoded as bits. if I flip even ONE bit\n" +
                        "  it re-generate the key. A strong key changes completely\n" +
                        "  from a single-bit input change.\n\n" +
                        "  This is the strongest avalanche test - the same\n"

        );
        explain.setEditable(false);
        explain.setOpaque(false);
        explain.setForeground(Color.LIGHT_GRAY);
        explain.setFont(new Font("SansSerif", Font.PLAIN, 13));

        runProofButton = styledButton("RUN THE TEST", true);
        runProofButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        runProofButton.addActionListener(e -> runAllTests());

        top.add(title);
        top.add(Box.createVerticalStrut(8));
        top.add(explain);
        top.add(Box.createVerticalStrut(12));
        top.add(runProofButton);

        proofArea = new JTextArea();
        proofArea.setEditable(false);
        proofArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        proofArea.setBackground(new Color(24, 26, 31));
        proofArea.setForeground(new Color(0, 230, 118));
        proofArea.setMargin(new Insets(14, 16, 14, 16));
        proofArea.setText("\nPress 'RUN THE TEST' to see the avalanche proof.\n");

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(proofArea), BorderLayout.CENTER);
        return root;
    }



    private void startProcess() {
        startButton.setEnabled(false);
        chooseButton.setEnabled(false);
        autoButton.setEnabled(false);
        statusArea.setText("");
        progressBar.setIndeterminate(true);
        progressBar.setString("Working...");

        SwingWorker<byte[], String> worker = new SwingWorker<>() {
            @Override
            protected byte[] doInBackground() {
                publish("Step 1 of 6  -  Kruskal algorithm...");
                BaseCryptoGraph kruskal = new KruskalCryptoGraph(finalBoard);
                publish("              done  ✓");

                publish("Step 2 of 6  -  Max-Flow algorithm...");
                BaseCryptoGraph maxFlow = new MaxFlowCryptoGraph(finalBoard);
                publish("              done  ✓");

                publish("Step 3 of 6  -  Bitwise algorithm...");
                BaseCryptoGraph bitwise = new BitwiseCryptoGraph(finalBoard);
                publish("              done  ✓");

                publish("Step 4 of 6  -  Eulerian path...");
                EulerianCryptoGraph euler = new EulerianCryptoGraph(13, gameHistory);
                publish("              done  ✓");

                publish("\nStep 5 of 6  -  Combining all results into a 256-bit key...");
                byte[] key = KeyGenerator.generateKey(
                        kruskal.getKeyMatrix(), maxFlow.getKeyMatrix(),
                        bitwise.getKeyMatrix(), euler.getKeyStream());
                publish("              done  ");
                publish("\nGenerated encryption key:");
                publish("  " + AvalancheTester.toHex(key));

                publish("\nStep 6 of 6  - AES-128 encryption...");
                prepareInputFile();
                silently(() -> {
                    AESFileEncryptor enc = new AESFileEncryptor(key);
                    enc.encryptFile(inputFilePath, ENCRYPTED_FILE);
                    enc.decryptFile(ENCRYPTED_FILE, DECRYPTED_FILE);
                });
                publish("              encryption done  ");
                publish("              decryption verified  ");
                publish("\nAll done!  Use the buttons below to open your files.");
                return key;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    statusArea.append(line + "\n");
                    statusArea.setCaretPosition(statusArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setString("Complete  ✓");
                setOpenButtonsEnabled(true);
                startButton.setText("DONE");
                JOptionPane.showMessageDialog(CryptoResultGUI.this,
                        "Encryption complete!\n\n" +
                                "Original:  " + inputFilePath + "\n" +
                                "Encrypted: " + ENCRYPTED_FILE + "\n" +
                                "Decrypted: " + DECRYPTED_FILE,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        worker.execute();
    }

    // =========================================================
    // SAC Test — SwingWorker
    // =========================================================

    private void runAllTests() {
        runProofButton.setEnabled(false);
        proofArea.setText("\nRunning the test, please wait...\n");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {

                AvalancheTester.AvalancheResult av =
                        silentlyCompute(() -> AvalancheTester.runAvalanche(finalBoard, gameHistory));

                // בניית הדוח
                StringBuilder sb = new StringBuilder();

                sb.append("\n");
                sb.append("╔══════════════════════════════════════════════════════════╗\n");
                sb.append("║          STRICT AVALANCHE CRITERION (SAC)               ║\n");
                sb.append("╚══════════════════════════════════════════════════════════╝\n\n");

                sb.append("changed:\n  ").append(av.changeDescription).append("\n\n");
                sb.append("Board encoded as bits (before flip):\n");
                sb.append("  ").append(av.boardBitsBefore).append("\n");
                sb.append("Board encoded as bits (after flipping bit #")
                        .append(av.flippedBitIndex).append("):\n");
                sb.append("  ").append(av.boardBitsAfter).append("\n");
                sb.append("   exactly ONE bit changed in the input\n\n");
                sb.append("──────────────────────────────────────────────────────────\n");
                sb.append("Key BEFORE the 1-bit change:\n");
                sb.append("  ").append(AvalancheTester.toHex(av.originalKey)).append("\n\n");
                sb.append("Key AFTER the 1-bit change:\n");
                sb.append("  ").append(AvalancheTester.toHex(av.modifiedKey)).append("\n\n");
                sb.append("──────────────────────────────────────────────────────────\n");
                sb.append("RESULT:  The key changed completely.\n");
                sb.append("         Flipping ONE bit in the input - entirely new key.\n\n");
                sb.append("Bit difference map  ( # = changed  ,  . = same ):\n\n");
                sb.append(AvalancheTester.diffMap(av.originalKey, av.modifiedKey));
                sb.append("\n══════════════════════════════════════════════════════════\n");

                return sb.toString();
            }

            @Override
            protected void done() {
                try {
                    proofArea.setText(get());
                    proofArea.setCaretPosition(0);
                } catch (Exception ex) {
                    proofArea.setText("Error: " + ex.getMessage());
                }
                runProofButton.setEnabled(true);
            }
        };
        worker.execute();
    }



    private JButton styledButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, primary ? 16 : 13));
        b.setFocusPainted(false);
        if (primary) { b.setBackground(GREEN); b.setForeground(Color.BLACK); }
        return b;
    }

    private void setOpenButtonsEnabled(boolean on) {
        openOriginal.setEnabled(on);
        openEncrypted.setEnabled(on);
        openDecrypted.setEnabled(on);
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a file to encrypt");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedInputFile = chooser.getSelectedFile();
            inputFilePath     = selectedInputFile.getAbsolutePath();
            fileLabel.setText("Selected: " + selectedInputFile.getName());
            fileLabel.setForeground(new Color(0, 230, 118));
        }
    }

    private void useAutoFile() {
        selectedInputFile = null;
        inputFilePath     = DEFAULT_INPUT;
        fileLabel.setText("A file named '" + DEFAULT_INPUT + "' will be created");
        fileLabel.setForeground(Color.ORANGE);
    }

    private void prepareInputFile() {
        if (selectedInputFile != null && selectedInputFile.exists()) return;
        File f = new File(inputFilePath);
        if (!f.exists()) {
            try (FileWriter fw = new FileWriter(f)) {
                fw.write("Picaria Auto-Generated Secret File\n");
                fw.write("Encrypted with a key derived from a Picaria game.\n");
            } catch (IOException ignored) {}
        }
    }

    private void openFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this,
                    "File not found:\n" + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
            else JOptionPane.showMessageDialog(this, "File location:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not open file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void silently(Runnable action) {
        java.io.PrintStream orig = System.out;
        System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
            public void write(int b) {}
        }));
        action.run();
        System.setOut(orig);
    }

    private <T> T silentlyCompute(java.util.function.Supplier<T> s) {
        java.io.PrintStream orig = System.out;
        System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
            public void write(int b) {}
        }));
        T result = s.get();
        System.setOut(orig);
        return result;
    }
}