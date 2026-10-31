package ui;

/** Panels implementing this reload their data from MySQL each time they are shown. */
public interface Refreshable {
    void refresh();
}
