package service;

import dao.AlumniDAO;
import model.Alumni;
import util.Choice;
import util.ServiceException;
import util.Sql;
import util.Validator;

import java.time.LocalDate;
import java.util.List;

public class AlumniService {
    private final AlumniDAO dao = new AlumniDAO();

    public List<Alumni> search(String keyword) { return Sql.call(() -> dao.findAll(keyword)); }

    public List<Choice> choices() { return Sql.call(dao::choices); }

    public void add(String first, String last, String gender, String dob, String email, String mobile,
                    String degree, String department, String gradYear) {
        Alumni a = build(null, first, last, gender, dob, email, mobile, degree, department, gradYear);
        Sql.exec(() -> {
            if (dao.emailExists(a.email(), null)) throw new ServiceException("An alumnus with this email already exists.");
            dao.insert(a);
        });
    }

    public void update(int id, String first, String last, String gender, String dob, String email, String mobile,
                       String degree, String department, String gradYear) {
        Alumni a = build(id, first, last, gender, dob, email, mobile, degree, department, gradYear);
        Sql.exec(() -> {
            if (dao.emailExists(a.email(), id)) throw new ServiceException("Another alumnus already uses this email.");
            if (!dao.update(a)) throw new ServiceException("This alumni record no longer exists.");
        });
    }

    public void delete(int id) {
        Sql.exec(() -> { if (!dao.delete(id)) throw new ServiceException("This alumni record no longer exists."); });
    }

    private Alumni build(Integer id, String first, String last, String gender, String dob, String email,
                         String mobile, String degree, String department, String gradYear) {
        String fn = Validator.required(first, "First name");
        String ln = Validator.required(last, "Last name");
        Validator.maxLen(fn, 50, "First name");
        Validator.maxLen(ln, 50, "Last name");
        String g = Validator.optional(gender);
        if (g != null && !g.matches("[MFO]")) throw new ServiceException("Gender must be M, F or O.");
        LocalDate d = Validator.optional(dob) == null ? null : Validator.date(dob, "Date of birth");
        if (d != null && !d.isBefore(LocalDate.now())) throw new ServiceException("Date of birth must be in the past.");
        String em = Validator.email(email);
        String mob = Validator.mobile(mobile);
        String deg = Validator.optional(degree);
        String dep = Validator.optional(department);
        Validator.maxLen(deg, 50, "Degree");
        Validator.maxLen(dep, 50, "Department");
        int year = Validator.integer(gradYear, "Graduation year");
        int max = LocalDate.now().getYear() + 6;
        if (year < 1950 || year > max) throw new ServiceException("Graduation year must be between 1950 and " + max + ".");
        return new Alumni(id, fn, ln, g, d, em, mob, deg, dep, year, null);
    }
}
