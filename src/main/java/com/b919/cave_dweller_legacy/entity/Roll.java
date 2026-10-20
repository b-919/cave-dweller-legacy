package com.b919.cave_dweller_legacy.entity;

public enum Roll {
    CHASE(0),
    STARE(1),
    FLEE(2),
    STROLL(3);

    public final int rollValue;

    Roll(int rollValue) {
        this.rollValue = rollValue;
    }

    public static Roll fromValue(int rollValue) {
        if (rollValue < 0 || rollValue >= values().length) {
            return STROLL;
        }
        return values()[rollValue];
    }
}
