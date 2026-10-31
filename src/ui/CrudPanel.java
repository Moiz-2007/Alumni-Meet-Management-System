package ui;

import util.ServiceException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared layout for every management screen: header + search, table, scrollable form, button bar.
 * Subclasses add their own form fields and say how to load / save / delete records.
 * All database calls go through Async, so they never block the Swing event thread.
 */
public abstract class CrudPanel<T> extends JPanel implements Refreshable {
    protected final DefaultTableModel model;
    protected final JTable table;
    protected final JTextField searchField = Theme.field(22);
    protected final JPanel formPanel = new JPanel(new GridBagLayout());
    protected final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    protected final JButton addBtn = Theme.button("Add", Theme.SUCCESS);
    protected final JButton updateBtn = Theme.button("Update", Theme.ACCENT);
    protected final JButton deleteBtn = Theme.button("Delete", Theme.DANGER);
    protected final JButton clearBtn = Theme.button("Clear", Theme.NEUTRAL);

    private final CardLayout cards = new CardLayout();
    private final JPanel tableHolder = new JPanel(cards);
    private final JLabel emptyLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel countLabel = new JLabel(" ");
    protected List<T> rows = new ArrayList<>();
    private int fieldCount = 0;

    protected CrudPanel(String title, String subtitle, String searchHint, String[] columns) {
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 22, 14, 22));

        // ---- header with title and search ----
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.TITLE);
        titleLabel.setForeground(Theme.PRIMARY);
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(Theme.NEUTRAL);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(titleLabel);
        titles.add(sub);

        searchField.setToolTipText(searchHint);
        JButton searchBtn = Theme.button("Search", Theme.PRIMARY);
        JButton allBtn = Theme.button("Show All", Theme.NEUTRAL);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        search.setOpaque(false);
        JLabel hint = new JLabel(searchHint + ":");
        hint.setForeground(Theme.NEUTRAL);
        search.add(hint);
        search.add(searchField);
        search.add(searchBtn);
        search.add(allBtn);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(search, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ---- table (or empty-state message) ----
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.styleTable(table);
        emptyLabel.setForeground(Theme.NEUTRAL);
        emptyLabel.setFont(Theme.BASE.deriveFont(15f));
        tableHolder.add(new JScrollPane(table), "table");
        tableHolder.add(emptyLabel, "empty");

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setOpaque(false);
        tableCard.add(tableHolder, BorderLayout.CENTER);
        countLabel.setForeground(Theme.NEUTRAL);
        tableCard.add(countLabel, BorderLayout.SOUTH);

        // ---- form + buttons in a scroll pane ----
        formPanel.setBackground(Color.WHITE);
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setBackground(Color.WHITE);
        formCard.add(formPanel, BorderLayout.CENTER);
        buttonBar.setBackground(Color.WHITE);
        formCard.add(buttonBar, BorderLayout.SOUTH);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xDDE3EA)), new EmptyBorder(8, 8, 4, 8)));
        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableCard, formScroll);
        split.setBorder(null);
        split.setResizeWeight(0.58);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);

        // ---- behaviour ----
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                T item = selected();
                if (item != null) fillForm(item);
            }
        });
        searchBtn.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        allBtn.addActionListener(e -> { searchField.setText(""); refresh(); });
        addBtn.addActionListener(e -> guarded(this::onAdd));
        updateBtn.addActionListener(e -> withSelection("update", this::onUpdate));
        deleteBtn.addActionListener(e -> withSelection("delete", item -> {
            if (Dialogs.confirm(this, "Delete the selected record permanently?")) onDelete(item);
        }));
        clearBtn.addActionListener(e -> { table.clearSelection(); clearForm(); });
    }

    // ---------- to be provided by each screen ----------
    protected abstract List<T> fetch(String keyword);
    protected abstract Object[] toRow(T item);
    protected abstract void fillForm(T item);
    protected abstract void clearForm();
    protected abstract void onAdd();
    protected abstract void onUpdate(T selected);
    protected abstract void onDelete(T selected);

    /** Call at the end of the subclass constructor, after all addField(...) calls. */
    protected void finish() {
        buttonBar.add(addBtn);
        buttonBar.add(updateBtn);
        buttonBar.add(deleteBtn);
        buttonBar.add(clearBtn);
    }

    @Override public void refresh() {
        String keyword = searchField.getText().trim();
        Async.fetch(this, () -> fetch(keyword), list -> {
            rows = list;
            model.setRowCount(0);
            for (T item : list) model.addRow(toRow(item));
            if (list.isEmpty()) {
                emptyLabel.setText(keyword.isEmpty() ? "No records yet. Use the form below to add one."
                                                     : "No records match \"" + keyword + "\".");
                cards.show(tableHolder, "empty");
            } else {
                cards.show(tableHolder, "table");
            }
            countLabel.setText(list.size() + " record(s)");
        });
    }

    // ---------- helpers for subclasses ----------
    protected T selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < rows.size() ? rows.get(modelRow) : null;
    }

    /** Runs a save action in the background, then shows a message, clears the form and reloads the table. */
    protected void perform(Runnable work, String successMessage) {
        Async.act(this, work, () -> {
            Dialogs.info(this, successMessage);
            table.clearSelection();
            clearForm();
            refresh();
        });
    }

    /** Like perform(), but keeps the selection and just reloads (used for quick actions such as Check-In Now). */
    protected void performQuiet(Runnable work) {
        Async.act(this, work, this::refresh);
    }

    protected void guarded(Runnable action) {
        try { action.run(); }
        catch (ServiceException ex) { Dialogs.warn(this, ex.getMessage()); }
    }

    protected void withSelection(String verb, java.util.function.Consumer<T> action) {
        T item = selected();
        if (item == null) { Dialogs.warn(this, "Select a row in the table first to " + verb + " it."); return; }
        guarded(() -> action.accept(item));
    }

    protected void addField(String label, JComponent component) {
        int row = fieldCount / 2;
        int col = (fieldCount % 2) * 2;
        placeLabel(label, col, row);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = col + 1; g.gridy = row; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 4, 14);
        formPanel.add(component, g);
        fieldCount++;
    }

    /** A field that spans the full form width (e.g. comments). Always starts a new row. */
    protected void addWideField(String label, JComponent component) {
        if (fieldCount % 2 == 1) fieldCount++;
        int row = fieldCount / 2;
        placeLabel(label, 0, row);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 1; g.gridy = row; g.gridwidth = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 4, 14);
        formPanel.add(component, g);
        fieldCount += 2;
    }

    private void placeLabel(String text, int col, int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = col; g.gridy = row; g.anchor = GridBagConstraints.EAST;
        g.insets = new Insets(4, 8, 4, 8);
        JLabel l = new JLabel(text);
        l.setFont(Theme.BOLD);
        formPanel.add(l, g);
    }
}
