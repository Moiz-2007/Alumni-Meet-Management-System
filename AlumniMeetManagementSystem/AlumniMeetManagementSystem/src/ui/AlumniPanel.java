package ui;

import model.Alumni;
import service.AlumniService;
import util.Fmt;

import javax.swing.*;
import java.util.List;

public class AlumniPanel extends CrudPanel<Alumni> {
    private final AlumniService service = new AlumniService();
    private final JTextField first = Theme.field(14), last = Theme.field(14), dob = Theme.field(14),
            email = Theme.field(14), mobile = Theme.field(14), degree = Theme.field(14),
            department = Theme.field(14), gradYear = Theme.field(14);
    private final JComboBox<String> gender = new JComboBox<>(new String[]{"", "M", "F", "O"});

    public AlumniPanel() {
        super("Alumni Management", "Add, update, search and remove alumni profiles",
              "Search name, email, department or year",
              new String[]{"ID", "First Name", "Last Name", "Gender", "DOB", "Age", "Email", "Mobile No", "Degree", "Department", "Grad. Year"});
        addField("First Name *", first);
        addField("Last Name *", last);
        addField("Gender", gender);
        addField("DOB (yyyy-MM-dd)", dob);
        addField("Email *", email);
        addField("Mobile No", mobile);
        addField("Degree", degree);
        addField("Department", department);
        addField("Graduation Year *", gradYear);
        finish();
    }

    @Override protected List<Alumni> fetch(String keyword) { return service.search(keyword); }

    @Override protected Object[] toRow(Alumni a) {
        return new Object[]{a.id(), a.firstName(), a.lastName(), Fmt.str(a.gender()), Fmt.str(a.dob()), Fmt.str(a.age()),
            a.email(), Fmt.str(a.mobileNo()), Fmt.str(a.degree()), Fmt.str(a.department()), Fmt.str(a.graduationYear())};
    }

    @Override protected void fillForm(Alumni a) {
        first.setText(a.firstName());
        last.setText(a.lastName());
        gender.setSelectedItem(Fmt.str(a.gender()));
        dob.setText(Fmt.str(a.dob()));
        email.setText(a.email());
        mobile.setText(Fmt.str(a.mobileNo()));
        degree.setText(Fmt.str(a.degree()));
        department.setText(Fmt.str(a.department()));
        gradYear.setText(Fmt.str(a.graduationYear()));
    }

    @Override protected void clearForm() {
        for (JTextField f : new JTextField[]{first, last, dob, email, mobile, degree, department, gradYear}) f.setText("");
        gender.setSelectedIndex(0);
    }

    @Override protected void onAdd() {
        String f = first.getText(), l = last.getText(), g = (String) gender.getSelectedItem(), d = dob.getText(),
               e = email.getText(), m = mobile.getText(), dg = degree.getText(), dp = department.getText(), y = gradYear.getText();
        perform(() -> service.add(f, l, g, d, e, m, dg, dp, y), "Alumni added successfully.");
    }

    @Override protected void onUpdate(Alumni sel) {
        String f = first.getText(), l = last.getText(), g = (String) gender.getSelectedItem(), d = dob.getText(),
               e = email.getText(), m = mobile.getText(), dg = degree.getText(), dp = department.getText(), y = gradYear.getText();
        perform(() -> service.update(sel.id(), f, l, g, d, e, m, dg, dp, y), "Alumni updated successfully.");
    }

    @Override protected void onDelete(Alumni sel) {
        perform(() -> service.delete(sel.id()), "Alumni deleted.");
    }
}
