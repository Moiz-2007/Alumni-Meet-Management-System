package ui;

import util.Choice;
import util.ServiceException;

import javax.swing.*;
import java.util.List;

/** Combo box filled with real database records (alumni, events, registrations). */
public class ChoiceCombo extends JComboBox<Choice> {
    private final String what;

    public ChoiceCombo(String what) {
        this.what = what;
        setSelectedIndex(-1);
    }

    public void setChoices(List<Choice> choices) {
        Integer keep = selectedId();
        removeAllItems();
        for (Choice c : choices) addItem(c);
        setSelectedIndex(-1);
        if (keep != null) select(keep);
    }

    public Integer selectedId() {
        Choice c = (Choice) getSelectedItem();
        return c == null ? null : c.id();
    }

    public int requireId() {
        Integer id = selectedId();
        if (id == null) throw new ServiceException("Please select " + what + ".");
        return id;
    }

    public void select(int id) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i).id() == id) { setSelectedIndex(i); return; }
        }
        setSelectedIndex(-1);
    }
}
