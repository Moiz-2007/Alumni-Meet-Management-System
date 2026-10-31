package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** One place for colours, fonts and component styling so every screen looks consistent. */
public final class Theme {
    private Theme() {}

    public static final Color SIDEBAR = new Color(0x16293F);
    public static final Color SIDEBAR_ACTIVE = new Color(0x2E86AB);
    public static final Color PRIMARY = new Color(0x1F3A5F);
    public static final Color ACCENT = new Color(0x2E86AB);
    public static final Color BG = new Color(0xF4F6F9);
    public static final Color ZEBRA = new Color(0xEEF3F8);
    public static final Color DANGER = new Color(0xC0392B);
    public static final Color SUCCESS = new Color(0x2E8B57);
    public static final Color NEUTRAL = new Color(0x6B7785);
    public static final Color TEXT = new Color(0x22303C);

    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 24);

    public static void apply() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { /* keep the default look and feel */ }
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "ComboBox.font", "Table.font",
                "TableHeader.font", "TextArea.font", "OptionPane.messageFont", "OptionPane.buttonFont", "ToolTip.font"}) {
            UIManager.put(key, BASE);
        }
        UIManager.put("Label.foreground", TEXT);
    }

    public static JButton button(String text, Color background) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(background);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(background.brighter()); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(background); }
        });
        return b;
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xC5CDD6)), new EmptyBorder(4, 6, 4, 6)));
        return f;
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(28);
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(0xDDE3EA));
        t.setSelectionBackground(ACCENT);
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setBackground(PRIMARY);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(BOLD);
        t.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, foc, r, c);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (sel) { setBackground(ACCENT); setForeground(Color.WHITE); }
                else { setBackground(r % 2 == 0 ? Color.WHITE : ZEBRA); setForeground(TEXT); }
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, zebra);
    }
}
