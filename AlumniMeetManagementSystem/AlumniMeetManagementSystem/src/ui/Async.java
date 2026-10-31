package ui;

import util.ServiceException;

import javax.swing.*;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Runs database work on a background thread (SwingWorker) so the window never freezes. */
public final class Async {
    private Async() {}

    /** Background work that returns a value; onSuccess runs on the Swing thread. */
    public static <T> void fetch(Component parent, Callable<T> work, Consumer<T> onSuccess) {
        fetch(parent, work, onSuccess, ex -> Dialogs.error(parent, message(ex)));
    }

    public static <T> void fetch(Component parent, Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (ExecutionException e) { onError.accept(e.getCause() == null ? e : e.getCause()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }.execute();
    }

    /** Background work with no result (insert / update / delete). */
    public static void act(Component parent, Runnable work, Runnable onSuccess) {
        fetch(parent, () -> { work.run(); return Boolean.TRUE; }, ok -> onSuccess.run());
    }

    public static String message(Throwable t) {
        if (t instanceof ServiceException && t.getMessage() != null) return t.getMessage();
        return "Unexpected error: " + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
    }
}
