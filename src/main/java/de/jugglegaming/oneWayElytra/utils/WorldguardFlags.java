package de.jugglegaming.oneWayElytra.utils;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

/**
 * Registers the custom WorldGuard flags. Must be called from {@code onLoad()}.
 */
public final class WorldguardFlags {

    public static StateFlag OWE_START;
    public static StateFlag OWE_ENTRY;
    public static StateFlag OWE_ITEM;

    private WorldguardFlags() {
    }

    public static void load() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        OWE_START = register(registry, "owe-start", false);
        OWE_ENTRY = register(registry, "owe-entry", true);
        OWE_ITEM = register(registry, "owe-item", true);
    }

    private static StateFlag register(FlagRegistry registry, String name, boolean defaultValue) {
        StateFlag flag = new StateFlag(name, defaultValue);
        try {
            registry.register(flag);
            return flag;
        } catch (FlagConflictException exception) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag stateFlag) {
                return stateFlag;
            }
            throw new IllegalStateException("WorldGuard flag '" + name + "' exists with an incompatible type", exception);
        }
    }
}
