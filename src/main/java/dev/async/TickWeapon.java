
package dev.async;

public enum TickWeapon {
    WEAPONS_ONLY,
    ALL_ITEMS;

    public TickWeapon next() {
        return this == WEAPONS_ONLY ? ALL_ITEMS : WEAPONS_ONLY;
    }
}
