package service;

import dao.EventDAO;
import model.Event;
import util.Choice;
import util.ServiceException;
import util.Sql;
import util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class EventService {
    private final EventDAO dao = new EventDAO();

    public List<Event> search(String keyword) { return Sql.call(() -> dao.findAll(keyword)); }

    public List<Choice> choices() { return Sql.call(dao::choices); }

    public void add(String name, String date, String venue, String start, String end, String capacity) {
        Event e = build(null, name, date, venue, start, end, capacity);
        Sql.exec(() -> dao.insert(e));
    }

    public void update(int id, String name, String date, String venue, String start, String end, String capacity) {
        Event e = build(id, name, date, venue, start, end, capacity);
        Sql.exec(() -> {
            int taken = dao.activeRegistrations(id);
            if (e.capacity() < taken)
                throw new ServiceException("Capacity cannot be lower than the " + taken + " seats already taken.");
            if (!dao.update(e)) throw new ServiceException("This event no longer exists.");
        });
    }

    public void delete(int id) {
        Sql.exec(() -> { if (!dao.delete(id)) throw new ServiceException("This event no longer exists."); });
    }

    private Event build(Integer id, String name, String date, String venue, String start, String end, String capacity) {
        String n = Validator.required(name, "Event name");
        Validator.maxLen(n, 100, "Event name");
        LocalDate d = Validator.date(date, "Event date");
        String v = Validator.optional(venue);
        Validator.maxLen(v, 100, "Venue");
        LocalTime s = Validator.time(start, "Start time");
        LocalTime e = Validator.time(end, "End time");
        if (!e.isAfter(s)) throw new ServiceException("End time must be later than start time.");
        int cap = Validator.integer(capacity, "Capacity");
        if (cap <= 0) throw new ServiceException("Capacity must be a positive number.");
        return new Event(id, n, d, v, s, e, cap, null);
    }
}
