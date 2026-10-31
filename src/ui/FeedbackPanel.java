package ui;

import model.Feedback;
import service.FeedbackService;
import service.RegistrationService;
import util.Fmt;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

public class FeedbackPanel extends CrudPanel<Feedback> {
    private final FeedbackService service = new FeedbackService();
    private final RegistrationService registrationService = new RegistrationService();
    private final ChoiceCombo registration = new ChoiceCombo("a registration");
    private final JComboBox<String> rating = new JComboBox<>(new String[]{"1", "2", "3", "4", "5"});
    private final JTextField date = Theme.field(14), comments = Theme.field(30);

    public FeedbackPanel() {
        super("Feedback Management", "Collect ratings (1-5) and comments for registrations",
              "Search comments, alumnus, event or rating",
              new String[]{"ID", "Reg. ID", "Alumnus", "Event", "Rating", "Comments", "Date"});
        rating.setSelectedIndex(4);
        date.setText(LocalDate.now().toString());
        addWideField("Registration *", registration);
        addField("Rating * (1-5)", rating);
        addField("Feedback Date * (yyyy-MM-dd)", date);
        addWideField("Comments", comments);
        finish();
    }

    @Override public void refresh() {
        Async.fetch(this, registrationService::choices, registration::setChoices);
        super.refresh();
    }

    @Override protected List<Feedback> fetch(String keyword) { return service.search(keyword); }

    @Override protected Object[] toRow(Feedback f) {
        return new Object[]{f.id(), f.registrationId(), f.alumniName(), f.meetName(), f.rating(), Fmt.str(f.comments()), f.date()};
    }

    @Override protected void fillForm(Feedback f) {
        registration.select(f.registrationId());
        rating.setSelectedItem(String.valueOf(f.rating()));
        comments.setText(Fmt.str(f.comments()));
        date.setText(String.valueOf(f.date()));
    }

    @Override protected void clearForm() {
        registration.setSelectedIndex(-1);
        rating.setSelectedIndex(4);
        comments.setText("");
        date.setText(LocalDate.now().toString());
    }

    @Override protected void onAdd() {
        int reg = registration.requireId();
        String r = (String) rating.getSelectedItem(), c = comments.getText(), d = date.getText();
        perform(() -> service.add(reg, r, c, d), "Feedback submitted.");
    }

    @Override protected void onUpdate(Feedback sel) {
        String r = (String) rating.getSelectedItem(), c = comments.getText(), d = date.getText();
        perform(() -> service.update(sel.id(), sel.registrationId(), r, c, d), "Feedback updated.");
    }

    @Override protected void onDelete(Feedback sel) {
        perform(() -> service.delete(sel.id()), "Feedback deleted.");
    }
}
