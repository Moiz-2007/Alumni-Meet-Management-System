package ui;

import database.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main window: navigation sidebar on the left, the selected screen in the centre. */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, JPanel> screens = new LinkedHashMap<>();

    public MainFrame() {
        super("Alumni Meet Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        screens.put("Dashboard", new DashboardPanel());
        screens.put("Alumni", new AlumniPanel());
        screens.put("Events", new EventPanel());
        screens.put("Registrations", new RegistrationPanel());
        screens.put("Attendance", new AttendancePanel());
        screens.put("Payments", new PaymentPanel());
        screens.put("Feedback", new FeedbackPanel());
        screens.put("Reports", new ReportsPanel());

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(18, 0, 14, 0));

        JLabel brand = new JLabel("<html><div style='text-align:left'>Alumni Meet<br>Management</div></html>");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brand.setForeground(Color.WHITE);
        brand.setBorder(new EmptyBorder(0, 20, 18, 10));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);

        for (Map.Entry<String, JPanel> entry : screens.entrySet()) {
            String name = entry.getKey();
            content.add(entry.getValue(), name);
            JButton b = new JButton(name);
            b.setFont(Theme.BASE.deriveFont(14f));
            b.setForeground(Color.WHITE);
            b.setBackground(Theme.SIDEBAR);
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setBorder(new EmptyBorder(11, 22, 11, 10));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> show(name));
            navButtons.put(name, b);
            sidebar.add(b);
        }

        sidebar.add(Box.createVerticalGlue());
        JLabel db = new JLabel("<html><div style='width:180px'>" + escape(DBConnection.describe()) + "</div></html>");
        db.setForeground(new Color(0x9FB3C8));
        db.setFont(Theme.BASE.deriveFont(11f));
        db.setBorder(new EmptyBorder(0, 20, 0, 10));
        db.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(db);

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        show("Dashboard");
    }

    private void show(String name) {
        cards.show(content, name);
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            e.getValue().setBackground(e.getKey().equals(name) ? Theme.SIDEBAR_ACTIVE : Theme.SIDEBAR);
        }
        JPanel screen = screens.get(name);
        if (screen instanceof Refreshable r) r.refresh(); // always show fresh data from MySQL
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
