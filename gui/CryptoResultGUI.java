package gui;

import crypto.AESFileEncryptor;
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
import java.io.PrintStream;
import java.util.List;

/**
 * CryptoResultGUI — החלון שמוצג אחרי סיום המשחק.
 *
 * מבנה (BorderLayout):
 *   NORTH  — אזור בחירת קובץ + כפתור התחלה
 *   CENTER — JTextArea עם scroll: כל פלט האלגוריתמים בזמן אמת
 *   SOUTH  — 3 כפתורים: פתיחת הקובץ המקורי / המוצפן / המפוענח
 *
 * התהליך רץ ב-SwingWorker ברקע — החלון נשאר מגיב והטקסט
 * מופיע הדרגתית תוך כדי ריצה.
 *
 * ספריות בשימוש: Swing + AWT בלבד.
 */
public class CryptoResultGUI extends JFrame {

    // נתוני המשחק שהסתיים
    private final Node[]     finalBoard;
    private final List<Move> gameHistory;

    // שמות קבצי פלט
    private static final String ENCRYPTED_FILE = "secret_encrypted.bin";
    private static final String DECRYPTED_FILE = "secret_decrypted.txt";
    private static final String DEFAULT_INPUT  = "secret.txt";

    // נתיב הקובץ שנבחר (null = ליצור אוטומטית)
    private File   selectedInputFile = null;
    private String inputFilePath     = DEFAULT_INPUT;

    // רכיבי GUI
    private JTextArea logArea;
    private JLabel    fileLabel;
    private JButton   startButton;
    private JButton   chooseButton;
    private JButton   autoButton;
    private JButton   openOriginalButton;
    private JButton   openEncryptedButton;
    private JButton   openDecryptedButton;

    // =========================================================
    // Constructor
    // =========================================================

    public CryptoResultGUI(Node[] finalBoard, List<Move> gameHistory) {
        this.finalBoard  = finalBoard;
        this.gameHistory = gameHistory;

        setTitle("Picaria Crypto - Encryption Process");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 720);
        setLayout(new BorderLayout());

        add(buildTopPanel(),    BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // =========================================================
    // NORTH — בחירת קובץ + התחלה
    // =========================================================

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(40, 44, 52));

        JLabel title = new JLabel("Encryption Key Generation & AES File Encryption",
                SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);

        // שורת בחירת קובץ
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        filePanel.setBackground(new Color(40, 44, 52));

        chooseButton = new JButton("Choose file to encrypt...");
        chooseButton.addActionListener(e -> chooseFile());

        autoButton = new JButton("Create file automatically");
        autoButton.addActionListener(e -> useAutoFile());

        fileLabel = new JLabel("No file selected (will create automatically)");
        fileLabel.setForeground(Color.LIGHT_GRAY);

        filePanel.add(chooseButton);
        filePanel.add(autoButton);
        filePanel.add(fileLabel);

        panel.add(filePanel, BorderLayout.CENTER);

        // כפתור התחלה
        startButton = new JButton("START ENCRYPTION PROCESS");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setBackground(new Color(46, 160, 67));
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startProcess());
        panel.add(startButton, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================
    // CENTER — לוג בזמן אמת
    // =========================================================

    private JScrollPane buildCenterPanel() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setBackground(new Color(24, 26, 31));
        logArea.setForeground(new Color(0, 230, 118));
        logArea.setText("Press 'START ENCRYPTION PROCESS' to begin...\n\n");

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return scroll;
    }

    // =========================================================
    // SOUTH — כפתורי פתיחת קבצים
    // =========================================================

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        panel.setBackground(new Color(40, 44, 52));

        openOriginalButton  = makeOpenButton("Open ORIGINAL file",  () -> inputFilePath);
        openEncryptedButton = makeOpenButton("Open ENCRYPTED file", () -> ENCRYPTED_FILE);
        openDecryptedButton = makeOpenButton("Open DECRYPTED file", () -> DECRYPTED_FILE);

        // מושבתים עד שהתהליך מסתיים
        openOriginalButton.setEnabled(false);
        openEncryptedButton.setEnabled(false);
        openDecryptedButton.setEnabled(false);

        panel.add(openOriginalButton);
        panel.add(openEncryptedButton);
        panel.add(openDecryptedButton);

        return panel;
    }

    private JButton makeOpenButton(String text, PathSupplier pathSupplier) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.addActionListener(e -> openFileInDefaultApp(pathSupplier.get()));
        return b;
    }

    // ממשק פנימי קטן לקבלת נתיב באופן עצל (lazy)
    private interface PathSupplier {
        String get();
    }

    // =========================================================
    // בחירת קובץ
    // =========================================================

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a file to encrypt");
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedInputFile = chooser.getSelectedFile();
            inputFilePath     = selectedInputFile.getAbsolutePath();
            fileLabel.setText("Selected: " + selectedInputFile.getName());
            fileLabel.setForeground(new Color(0, 230, 118));
        }
    }

    private void useAutoFile() {
        selectedInputFile = null;
        inputFilePath     = DEFAULT_INPUT;
        fileLabel.setText("Will create '" + DEFAULT_INPUT + "' automatically");
        fileLabel.setForeground(Color.ORANGE);
    }

    // =========================================================
    // התהליך הראשי — SwingWorker (רץ ברקע)
    // =========================================================

    private void startProcess() {
        // מנטרל כפתורים בזמן ריצה
        startButton.setEnabled(false);
        chooseButton.setEnabled(false);
        autoButton.setEnabled(false);
        logArea.setText("");

        // מפנה את System.out ל-GUI
        PrintStream originalOut = System.out;
        PrintStream guiStream   = new PrintStream(new GuiConsole(logArea), true);
        System.setOut(guiStream);

        // SwingWorker: doInBackground רץ ב-thread נפרד,
        // done() רץ חזרה ב-Event Dispatch Thread
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                runCryptoPipeline();
                return null;
            }

            @Override
            protected void done() {
                // משחזר את System.out המקורי
                System.setOut(originalOut);

                // מפעיל את כפתורי הפתיחה
                openOriginalButton.setEnabled(true);
                openEncryptedButton.setEnabled(true);
                openDecryptedButton.setEnabled(true);

                startButton.setText("PROCESS COMPLETE");
                startButton.setBackground(new Color(80, 80, 80));

                JOptionPane.showMessageDialog(
                        CryptoResultGUI.this,
                        "Encryption complete!\n\n" +
                                "Original  : " + inputFilePath + "\n" +
                                "Encrypted : " + ENCRYPTED_FILE + "\n" +
                                "Decrypted : " + DECRYPTED_FILE + "\n\n" +
                                "Use the buttons below to open each file.",
                        "Done",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        };
        worker.execute();
    }

    /**
     * ה-pipeline המלא — אותו תהליך כמו ב-GameRules,
     * אבל כל הפלט מופנה ל-GUI דרך GuiConsole.
     */
    private void runCryptoPipeline() {

        System.out.println("[1/4] Running Kruskal (MST + shortest paths)...");
        BaseCryptoGraph kruskal = new KruskalCryptoGraph(finalBoard);
        kruskal.printKeyMatrix();

        System.out.println("\n[2/4] Running MaxFlow (Edmonds-Karp)...");
        BaseCryptoGraph maxFlow = new MaxFlowCryptoGraph(finalBoard);
        maxFlow.printKeyMatrix();

        System.out.println("\n[3/4] Running Bitwise (Dijkstra-enhanced)...");
        BaseCryptoGraph bitwise = new BitwiseCryptoGraph(finalBoard);
        bitwise.printKeyMatrix();

        System.out.println("\n[4/4] Running Eulerian path (Hierholzer)...");
        EulerianCryptoGraph euler = new EulerianCryptoGraph(13, gameHistory);
        euler.printKeyStream();

        System.out.println("\n>>> Combining all outputs into 256-bit key...");
        byte[] finalKey = KeyGenerator.generateKey(
                kruskal.getKeyMatrix(),
                maxFlow.getKeyMatrix(),
                bitwise.getKeyMatrix(),
                euler.getKeyStream()
        );
        KeyGenerator.printKey(finalKey);

        // אם המשתמש בחר קובץ — מוודאים שהוא קיים, אחרת יוצרים
        prepareInputFile();

        System.out.println("\n>>> Launching AES-128 CBC file encryption...");
        AESFileEncryptor encryptor = new AESFileEncryptor(finalKey);
        encryptor.encryptFile(inputFilePath, ENCRYPTED_FILE);
        encryptor.decryptFile(ENCRYPTED_FILE, DECRYPTED_FILE);
    }

    /**
     * אם המשתמש לא בחר קובץ — יוצר קובץ ברירת-מחדל.
     * (ה-AESFileEncryptor גם יודע ליצור, אבל כאן זה מפורש ל-GUI)
     */
    private void prepareInputFile() {
        if (selectedInputFile != null && selectedInputFile.exists()) {
            System.out.println("\nUsing user-selected file: " + inputFilePath);
            return;
        }

        File f = new File(inputFilePath);
        if (!f.exists()) {
            System.out.println("\nNo file selected - creating '" + inputFilePath + "'...");
            try (FileWriter fw = new FileWriter(f)) {
                fw.write("Picaria Auto-Generated Secret File\n");
                fw.write("===================================\n");
                fw.write("This file was created automatically and encrypted\n");
                fw.write("with a key derived from a Picaria game.\n");
            } catch (IOException e) {
                System.out.println("ERROR creating file: " + e.getMessage());
            }
        }
    }

    // =========================================================
    // פתיחת קובץ בתוכנת ברירת-המחדל של המחשב
    // =========================================================

    private void openFileInDefaultApp(String path) {
        File file = new File(path);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this,
                    "File not found:\n" + file.getAbsolutePath(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Desktop not supported on this system.\nFile location:\n"
                                + file.getAbsolutePath(),
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not open file:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}