package org.workswap.core.datasource.central.model.enums;

public enum Role {
    USER(1),
    PREMIUM(2),
    BUSINESS(3),
    ADMIN(4);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isAtLeast(Role other) {
        return this.level >= other.level;
    }
}