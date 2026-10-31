package ui;

import model.Attendance;
import service.AttendanceService;
import service.RegistrationService;
import util.Fmt;

import javax.swing.*;
import java.util.List;

public class AttendancePanel extends CrudPanel<Attendance> {
    private final AttendanceService service = new AttendanceService();
    private final RegistrationService registrationService = new RegistrationService();
    private final ChoiceCombo registration = new ChoiceCombo("a registration");
    private final JComboBox<String> status = new JComboBox<>(new String[]{"Present", "Absent"});
    private final JTextField checkIn = Theme.field(14), checkOut = Theme.field(14);

    public AttendancePanel() {
        super("Attendance Management", "One attendance record per registration, stored in its own table",
              "Search alumnus, event or status",
              new String[]{"ID", "Reg. ID", "Alumnus", "Event", "Status", "Check-In", "Check-Out"});
        addWideField("Registration *", registration);
        addField("Status *", status);
        addField("Check-In (yyyy-MM-dd HH:mm)", checkIn);
        addField("Check-Out (yyyy-MM-dd HH:mm)", checkOut);
        addButtonsAndFinish();
    }

    private void addButtonsAndFinish() {
        addBtn.setText("Create Record");
        updateBtn.setText("Update");
        finish();
        JButton inNow = Theme.button("Check-In Now", Theme.PRIMARY);
        JButton outNow = Theme.button("Check-Out Now", Theme.PRIMARY);
        inNow.addActionListener(e -> withSelection("check in", sel -> performQuiet(() -> service.checkInNow(sel.id()))));
        outNow.addActionListener(e -> withSelection("check out", sel -> performQuiet(() -> service.checkOutNow(sel.id()))));
        buttonBar.add(inNow);
        buttonBar.add(outNow);
    }

    @Override public void refresh() {
        Async.fetch(this, registrationService::choices, registration::setChoices);
        super.refresh();
    }

    @Override protected List<Attendance> fetch(String keyword) { return service.search(keyword); }

    @Override protected Object[] toRow(Attendance a) {
        return new Object[]{a.id(), a.registrationId(), a.alumniName(), a.meetName(), a.status(), Fmt.dt(a.checkIn()), Fmt.dt(a.checkOut())};
    }

    @Override protected void fillForm(Attendance a) {
        registration.select(a.registrationId());
        status.setSelectedItem(a.status());
        checkIn.setText(Fmt.dt(a.checkIn()));
        checkOut.setText(Fmt.dt(a.checkOut()));
    }

    @Override protected void clearForm() {
        registration.setSelectedIndex(-1);
        status.setSelectedIndex(0);
        checkIn.setText("");
        checkOut.setText("");
    }

    @Override protected void onAdd() {
        int reg = registration.requireId();
        String s = (String) status.getSelectedItem(), in = checkIn.getText(), out = checkOut.getText();
        perform(() -> service.add(reg, s, in, out), "Attendance record created.");
    }

    @Override protected void onUpdate(Attendance sel) {
        String s = (String) status.getSelectedItem(), in = checkIn.getText(), out = checkOut.getText();
        perform(() -> service.update(sel.id(), sel.registrationId(), s, in, out), "Attendance updated.");
    }

    @Override protected void onDelete(Attendance sel) {
        perform(() -> service.delete(sel.id()), "Attendance record deleted.");
    }
}
