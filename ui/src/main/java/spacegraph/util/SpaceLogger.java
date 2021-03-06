package spacegraph.util;

import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.util.function.Supplier;

public interface SpaceLogger {

    /** new log message will likely replace an existing log message by the same key. */
    default void log(@Nullable Object key, float duration /* seconds */, Level level, Supplier<String> message) {
        if (logging(level))
            System.out.println(message.get());
    }

    default boolean logging(Level level) {
        return true;
    }

    default void debug(@Nullable Object key, float duration /* seconds */, Supplier<String> message) {
        log(key, duration, Level.DEBUG, message);
    }
    default void debug(@Nullable Object key, float duration /* seconds */, String message) {
        log(key, duration, Level.DEBUG, ()->message);
    }
    default void info(@Nullable Object key, float duration /* seconds */, Supplier<String> message) {
        log(key, duration, Level.INFO, message);
    }
    default void info(@Nullable Object key, float duration /* seconds */, String message) {
        log(key, duration, Level.INFO, ()->message);
    }

    default void error(@Nullable Object key, float duration /* seconds */, Throwable error) {
        log(key, duration, Level.ERROR, error::toString);
    }

    default void error(@Nullable Object key, float duration /* seconds */, Supplier<String> message) {
        log(key, duration, Level.ERROR, message);
    }
    default void error(@Nullable Object key, float duration /* seconds */, String message) {
        log(key, duration, Level.ERROR, ()->message);
    }

}
