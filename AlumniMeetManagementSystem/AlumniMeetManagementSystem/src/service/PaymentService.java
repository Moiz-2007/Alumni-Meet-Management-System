package service;

import dao.PaymentDAO;
import model.Payment;
import util.ServiceException;
import util.Sql;
import util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Academic tracking only - this records payments, it does not process real money. */
public class PaymentService {
    public static final List<String> MODES = List.of("UPI", "Card", "Cash", "Net Banking");
    public static final List<String> STATUSES = List.of("Paid", "Pending", "Failed", "Refunded");
    private final PaymentDAO dao = new PaymentDAO();

    public List<Payment> search(String keyword) { return Sql.call(() -> dao.findAll(keyword)); }

    public void add(int registrationId, String txn, String amount, String mode, String date, String status) {
        Payment p = build(null, registrationId, txn, amount, mode, date, status);
        Sql.exec(() -> {
            if (dao.transactionExists(p.transactionId(), null)) throw new ServiceException("This transaction ID is already recorded.");
            dao.insert(p);
        });
    }

    public void update(int id, int registrationId, String txn, String amount, String mode, String date, String status) {
        Payment p = build(id, registrationId, txn, amount, mode, date, status);
        Sql.exec(() -> {
            if (dao.transactionExists(p.transactionId(), id)) throw new ServiceException("Another payment already uses this transaction ID.");
            if (!dao.update(p)) throw new ServiceException("This payment no longer exists.");
        });
    }

    public void delete(int id) {
        Sql.exec(() -> { if (!dao.delete(id)) throw new ServiceException("This payment no longer exists."); });
    }

    private Payment build(Integer id, int registrationId, String txn, String amount, String mode, String date, String status) {
        String t = Validator.required(txn, "Transaction ID");
        Validator.maxLen(t, 50, "Transaction ID");
        BigDecimal a = Validator.amount(amount, "Amount");
        if (mode == null || !MODES.contains(mode)) throw new ServiceException("Choose a payment mode.");
        LocalDate d = Validator.date(date, "Payment date");
        if (status == null || !STATUSES.contains(status)) throw new ServiceException("Choose a payment status.");
        return new Payment(id, registrationId, t, a, mode, d, status, null, null);
    }
}
