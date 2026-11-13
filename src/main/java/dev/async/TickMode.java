
package dev.async;

public enum TickMode {
    NORMAL,
    FAST;

    public TickMode next() {
        return this == NORMAL ? FAST : NORMAL;
    }
}
