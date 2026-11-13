package dev.async;

public enum TargetMode {
    MOBS,
    PLAYERS;

    public TargetMode next() {
        return this == MOBS ? PLAYERS : MOBS;
    }
}
