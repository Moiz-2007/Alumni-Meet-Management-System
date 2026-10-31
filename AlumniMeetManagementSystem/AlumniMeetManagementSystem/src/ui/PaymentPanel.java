package ui;

import model.Payment;
import service.PaymentService;
import service.RegistrationService;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

/** Tracks payments for the academic project - it does not talk to any bank or UPI gateway. */
public class PaymentPanel extends CrudPanel<Payment> {
    private final PaymentService service = new PaymentService();
    private final RegistrationService registrationService = new RegistrationService();
    private final ChoiceCombo registration = new ChoiceCombo("a registration");
    private final JTextField txn = Theme.field(14), amount = Theme.field(14), date = Theme.field(14);
    private final JComboBox<String> mode = new JComboBox<>(PaymentService.MODES.toArray(new String[0]));
    private final JComboBox<String> status = new JComboBox<>(PaymentService.STATUSES.toArray(new String[0]));

    public PaymentPanel() {
        super("Payment Management", "Record and track payments (academic tracking only - no real transactions)",
              "Search transaction ID, registration or status",
              new String[]{"ID", "Reg. ID", "Alumnus", "Event", "Transaction ID", "Amount", "Mode", "Date", "Status"});
        addWideField("Registration *", registration);
        addField("Transaction ID *", txn);
        addField("Amount *", amount);
        addField("Mode *", mode);
        addField("Payment Date * (yyyy-MM-dd)", date);
        addField("Status *", status);
        date.setText(LocalDate.now().toString());
        finish();
    }

    @Override public void refresh() {
        Async.fetch(this, registrationService::choices, registration::setChoices);
        super.refresh();
    }

    @Override protected List<Payment> fetch(String keyword) { return service.search(keyword); }

    @Override protected Object[] toRow(Payment p) {
        return new Object[]{p.id(), p.registrationId(), p.alumniName(), p.meetName(), p.transactionId(), p.amount(), p.mode(), p.date(), p.status()};
    }

    @Override protected void fillForm(Payment p) {
        registration.select(p.registrationId());
        txn.setText(p.transactionId());
        amount.setText(p.amount().toPlainString());
        mode.setSelectedItem(p.mode());
        date.setText(String.valueOf(p.date()));
        status.setSelectedItem(p.status());
    }

    @Override protected void clearForm() {
        registration.setSelectedIndex(-1);
        txn.setText("");
        amount.setText("");
        mode.setSelectedIndex(0);
        date.setText(LocalDate.now().toString());
        status.setSelectedIndex(0);
    }

    @Override protected void onAdd() {
        int reg = registration.requireId();
        String t = txn.getText(), a = amount.getText(), m = (String) mode.getSelectedItem(), d = date.getText(), s = (String) status.getSelectedItem();
        perform(() -> service.add(reg, t, a, m, d, s), "Payment recorded.");
    }

    @Override protected void onUpdate(Payment sel) {
        String t = txn.getText(), a = amount.getText(), m = (String) mode.getSelectedItem(), d = date.getText(), s = (String) status.getSelectedItem();
        perform(() -> service.update(sel.id(), sel.registrationId(), t, a, m, d, s), "Payment updated.");
    }

    @Override protected void onDelete(Payment sel) {
        perform(() -> service.delete(sel.id()), "Payment deleted.");
    }
}
