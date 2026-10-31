package ui;

import dao.ReportDAO;
import service.ReportService;
import util.Fmt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ReportsPanel extends JPanel implements Refreshable {
    private final ReportService service = new ReportService();
    private final JComboBox<ReportDAO.Report> picker = new JComboBox<>();
    private final JTextArea sqlArea = new JTextArea(7, 60);
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel info = new JLabel(" ");
    private boolean loading = false;

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 22, 14, 22));

        JLabel title = new JLabel("Reports & SQL Queries");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.PRIMARY);
        JLabel sub = new JLabel("Each report runs a real query (JOIN, GROUP BY, aggregates, subqueries) against MySQL");
        sub.setForeground(Theme.NEUTRAL);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(sub);

        for (ReportDAO.Report r : service.reports()) picker.addItem(r);
        JButton run = Theme.button("Run Report", Theme.ACCENT);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        controls.add(picker);
        controls.add(run);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        sqlArea.setEditable(false);
        sqlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        sqlArea.setBackground(new Color(0x1E2A38));
        sqlArea.setForeground(new Color(0xD7E3F0));
        sqlArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JTable table = new JTable(model);
        Theme.styleTable(table);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(sqlArea), new JScrollPane(table));
        split.setResizeWeight(0.25);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        info.setForeground(Theme.NEUTRAL);
        add(info, BorderLayout.SOUTH);

        picker.addActionListener(e -> { if (!loading) runSelected(); });
        run.addActionListener(e -> runSelected());
    }

    @Override public void refresh() { runSelected(); }

    private void runSelected() {
        ReportDAO.Report report = (ReportDAO.Report) picker.getSelectedItem();
        if (report == null) return;
        sqlArea.setText(report.sql());
        sqlArea.setCaretPosition(0);
        loading = true;
        Async.fetch(this, () -> service.run(report), result -> {
            loading = false;
            model.setRowCount(0);
            model.setColumnIdentifiers(result.columns().toArray());
            for (Object[] row : result.rows()) {
                Object[] shown = new Object[row.length];
                for (int i = 0; i < row.length; i++) shown[i] = Fmt.str(row[i]);
                model.addRow(shown);
            }
            info.setText(result.rows().isEmpty() ? "This report returned no rows." : result.rows().size() + " row(s)");
        }, ex -> {
            loading = false;
            Dialogs.error(this, Async.message(ex));
        });
    }
}
