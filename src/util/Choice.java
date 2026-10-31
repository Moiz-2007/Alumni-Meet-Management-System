package util;

/** An id + label pair used to fill combo boxes with real database records. */
public record Choice(int id, String label) {
    @Override public String toString() { return label; }
}
