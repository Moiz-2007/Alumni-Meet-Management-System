package ui;

import model.Registration;
import service.AlumniService;
import service.EventService;
import service.RegistrationService;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

public class RegistrationPanel extends CrudPanel<Registration> {
    private final RegistrationService service = new RegistrationService();
    private final AlumniService alumniService = new AlumniService();
    private final EventService eventService = new EventService();
    private final ChoiceCombo alumni = new ChoiceCombo("an alumnus");
    private final ChoiceCombo event = new ChoiceCombo("an event");
    private final JTextField date = Theme.field(14);
    private final JComboBox<String> status = new JComboBox<>(new String[]{"Pending", "Confirmed", "Cancelled"});

    public RegistrationPanel() {
        super("Registration Management", "Register alumni for events (capacity and duplicates are enforced)",
              "Search alumnus, event or status",
              new String[]{"Reg. ID", "Alumnus", "Event", "Registered On", "Status"});
        date.setEditable(false);
        date.setText(LocalDate.now().toString());
        addField("Alumnus *", alumni);
        addField("Event *", event);
        addField("Status", status);
        addField("Registration Date", date);
        updateBtn.setText("Update Status");
        deleteBtn.setText("Delete Registration");
        finish();
    }

    @Override public void refresh() {
        Async.fetch(this, alumniService::choices, alumni::setChoices);
        Async.fetch(this, eventService::choices, event::setChoices);
        super.refresh();
    }

    @Override protected List<Registration> fetch(String keyword) { return service.search(keyword); }

    @Override protected Object[] toRow(Registration r) {
        return new Object[]{r.id(), r.alumniName(), r.meetName(), r.date(), r.status()};
    }

    @Override protected void fillForm(Registration r) {
        alumni.select(r.alumniId());
        event.select(r.meetId());
        status.setSelectedItem(r.status());
        date.setText(String.valueOf(r.date()));
    }

    @Override protected void clearForm() {
        alumni.setSelectedIndex(-1);
        event.setSelectedIndex(-1);
        status.setSelectedIndex(0);
        date.setText(LocalDate.now().toString());
    }

    @Override protected void onAdd() {
        int a = alumni.requireId(), e = event.requireId();
        String s = (String) status.getSelectedItem();
        perform(() -> service.register(a, e, s), "Registration saved.");
    }

    @Override protected void onUpdate(Registration sel) {
        String s = (String) status.getSelectedItem();
        perform(() -> service.updateStatus(sel.id(), s), "Registration status updated.");
    }

    @Override protected void onDelete(Registration sel) {
        perform(() -> service.delete(sel.id()), "Registration deleted.");
    }
}
