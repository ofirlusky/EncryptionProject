package gui;

import javax.swing.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * GuiConsole — OutputStream שמפנה כל פלט (System.out) ל-JTextArea.
 *
 * השימוש: עוטפים את ה-OutputStream הזה ב-PrintStream ומחליפים
 * את System.out, כך שכל System.out.println של האלגוריתמים
 * מופיע אוטומטית ב-GUI במקום בטרמינל.
 *
 * השרשור ל-JTextArea נעשה דרך SwingUtilities.invokeLater
 * כדי שהעדכון יקרה ב-Event Dispatch Thread (בטוח ל-Swing).
 */
public class GuiConsole extends OutputStream {

    private final JTextArea textArea;
    private final StringBuilder buffer = new StringBuilder();

    public GuiConsole(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) {
        // צובר תו אחד; כשמגיע \n שולח את כל השורה ל-GUI
        buffer.append((char) b);
        if (b == '\n') {
            flushBuffer();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        buffer.append(new String(b, off, len, StandardCharsets.UTF_8));
        if (buffer.indexOf("\n") >= 0) {
            flushBuffer();
        }
    }

    private void flushBuffer() {
        final String text = buffer.toString();
        buffer.setLength(0);

        // עדכון ה-GUI חייב לקרות ב-Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            // גלילה אוטומטית לסוף
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}