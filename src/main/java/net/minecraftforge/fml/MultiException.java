package net.minecraftforge.fml;

import java.util.List;
import java.util.stream.Collectors;

public class MultiException extends RuntimeException {

    protected final List<? extends Throwable> wrappedExceptions;

    public MultiException(final List<? extends Throwable> exceptions) {
        this.wrappedExceptions = exceptions;
        exceptions.forEach(this::addSuppressed);
    }

    @Override
    public String getMessage() {
        return "Loading errors encountered: " + this.wrappedExceptions.stream()
            .map(Throwable::toString)
            .collect(Collectors.joining(",\n\t", "[\n\t", "\n]"));
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }
}
