package ui;

import model.Event;
import service.EventService;
import util.Fmt;

import javax.swing.*;
import java.util.List;

public class EventPanel extends CrudPanel<Event> {
    private final EventService service = new EventService();
    private final JTextField name = Theme.field(14), date = Theme.field(14), venue = Theme.field(14),
            start = Theme.field(14), end = Theme.field(14), capacity = Theme.field(14);

    public EventPanel() {
        super("Event Management", "Create alumni meets and track seats", "Search name, venue or date (e.g. 2026-10)",
              new String[]{"ID", "Event Name", "Date", "Venue", "Start", "End", "Capacity", "Registered", "Available"});
        addField("Event Name *", name);
        addField("Date * (yyyy-MM-dd)", date);
        addField("Venue", venue);
        addField("Capacity *", capacity);
        addField("Start * (HH:mm)", start);
        addField("End * (HH:mm)", end);
        finish();
    }

    @Override protected List<Event> fetch(String keyword) { return service.search(keyword); }

    @Override protected Object[] toRow(Event e) {
        int registered = e.registered() == null ? 0 : e.registered();
        return new Object[]{e.id(), e.name(), e.date(), Fmt.str(e.venue()), Fmt.str(e.startTime()), Fmt.str(e.endTime()),
            e.capacity(), registered, e.capacity() - registered};
    }

    @Override protected void fillForm(Event e) {
        name.setText(e.name());
        date.setText(Fmt.str(e.date()));
        venue.setText(Fmt.str(e.venue()));
        start.setText(shortTime(e.startTime()));
        end.setText(shortTime(e.endTime()));
        capacity.setText(String.valueOf(e.capacity()));
    }

    private String shortTime(java.time.LocalTime t) {
        return t == null ? "" : t.toString(); // LocalTime prints HH:mm when seconds are zero
    }

    @Override protected void clearForm() {
        for (JTextField f : new JTextField[]{name, date, venue, start, end, capacity}) f.setText("");
    }

    @Override protected void onAdd() {
        String n = name.getText(), d = date.getText(), v = venue.getText(), s = start.getText(), e = end.getText(), c = capacity.getText();
        perform(() -> service.add(n, d, v, s, e, c), "Event added successfully.");
    }

    @Override protected void onUpdate(Event sel) {
        String n = name.getText(), d = date.getText(), v = venue.getText(), s = start.getText(), e = end.getText(), c = capacity.getText();
        perform(() -> service.update(sel.id(), n, d, v, s, e, c), "Event updated successfully.");
    }

    @Override protected void onDelete(Event sel) {
        perform(() -> service.delete(sel.id()), "Event deleted.");
    }
}
