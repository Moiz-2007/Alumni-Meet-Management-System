package ui;

import database.DBConnection;
import model.DashboardStats;
import service.DashboardService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardPanel extends JPanel implements Refreshable {
    private final DashboardService service = new DashboardService();
    private final JLabel[] values = new JLabel[6];
    private final JLabel status = new JLabel(" ");
    private static final String[] CAPTIONS = {"Total Alumni", "Total Events", "Total Registrations",
        "Attendance Records", "Successful Payments", "Feedback Records"};
    private static final Color[] COLORS = {new Color(0x2E86AB), new Color(0x8E5BA8), new Color(0x2E8B57),
        new Color(0xE08E2B), new Color(0x1F3A5F), new Color(0xC0577B)};

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("Dashboard");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.PRIMARY);
        JLabel sub = new JLabel("Live counts read from MySQL every time this screen opens");
        sub.setForeground(Theme.NEUTRAL);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);
        JButton refreshBtn = Theme.button("Refresh", Theme.ACCENT);
        refreshBtn.addActionListener(e -> refresh());
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setOpaque(false);
        for (int i = 0; i < 6; i++) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, COLORS[i]), new EmptyBorder(18, 18, 18, 18)));
            values[i] = new JLabel("-");
            values[i].setFont(new Font("Segoe UI", Font.BOLD, 40));
            values[i].setForeground(COLORS[i]);
            JLabel caption = new JLabel(CAPTIONS[i]);
            caption.setForeground(Theme.NEUTRAL);
            card.add(values[i], BorderLayout.CENTER);
            card.add(caption, BorderLayout.SOUTH);
            grid.add(card);
        }
        add(grid, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    @Override public void refresh() {
        status.setForeground(Theme.NEUTRAL);
        status.setText("Loading...");
        Async.fetch(this, service::stats, s -> {
            show(s);
            status.setText("Connected to " + DBConnection.describe());
        }, ex -> {
            for (JLabel v : values) v.setText("-");
            status.setForeground(Theme.DANGER);
            status.setText("Database problem: " + Async.message(ex));
        });
    }

    private void show(DashboardStats s) {
        long[] n = {s.alumni(), s.events(), s.registrations(), s.attendance(), s.paidPayments(), s.feedback()};
        for (int i = 0; i < 6; i++) values[i].setText(String.valueOf(n[i]));
    }
}
