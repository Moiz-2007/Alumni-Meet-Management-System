package service;

import dao.FeedbackDAO;
import model.Feedback;
import util.ServiceException;
import util.Sql;
import util.Validator;

import java.time.LocalDate;
import java.util.List;

public class FeedbackService {
    private final FeedbackDAO dao = new FeedbackDAO();

    public List<Feedback> search(String keyword) { return Sql.call(() -> dao.findAll(keyword)); }

    public void add(int registrationId, String rating, String comments, String date) {
        Feedback f = build(null, registrationId, rating, comments, date);
        Sql.exec(() -> dao.insert(f));
    }

    public void update(int id, int registrationId, String rating, String comments, String date) {
        Feedback f = build(id, registrationId, rating, comments, date);
        Sql.exec(() -> { if (!dao.update(f)) throw new ServiceException("This feedback no longer exists."); });
    }

    public void delete(int id) {
        Sql.exec(() -> { if (!dao.delete(id)) throw new ServiceException("This feedback no longer exists."); });
    }

    private Feedback build(Integer id, int registrationId, String rating, String comments, String date) {
        int r = Validator.integer(rating, "Rating");
        if (r < 1 || r > 5) throw new ServiceException("Rating must be between 1 and 5.");
        String c = Validator.optional(comments);
        Validator.maxLen(c, 255, "Comments");
        LocalDate d = Validator.date(date, "Feedback date");
        return new Feedback(id, registrationId, r, c, d, null, null);
    }
}
