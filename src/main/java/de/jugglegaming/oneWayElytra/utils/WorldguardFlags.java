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

    public static final String START_FLAG = "owe-start";
    public static final String ENTRY_FLAG = "owe-entry";
    public static final String ITEM_FLAG = "owe-item";

    public static StateFlag OWE_START;
    public static StateFlag OWE_ENTRY;
    public static StateFlag OWE_ITEM;

    private WorldguardFlags() {
    }

    public static void load() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        OWE_START = register(registry, START_FLAG, false);
        OWE_ENTRY = register(registry, ENTRY_FLAG, true);
        OWE_ITEM = register(registry, ITEM_FLAG, true);
    }

    /**
     * Returns the cached flag, falling back to a registry lookup.
     *
     * <p>This makes the hook resilient if it is created after the flags were
     * registered by another (re)load of the plugin.</p>
     */
    public static StateFlag getStart() {
        return resolve(OWE_START, START_FLAG);
    }

    public static StateFlag getEntry() {
        return resolve(OWE_ENTRY, ENTRY_FLAG);
    }

    public static StateFlag getItem() {
        return resolve(OWE_ITEM, ITEM_FLAG);
    }

    private static StateFlag resolve(StateFlag cached, String name) {
        if (cached != null) {
            return cached;
        }
        Flag<?> existing = WorldGuard.getInstance().getFlagRegistry().get(name);
        if (existing instanceof StateFlag stateFlag) {
            return stateFlag;
        }
        return null;
    }

    private static StateFlag register(FlagRegistry registry, String name, boolean defaultValue) {
        StateFlag flag = new StateFlag(name, defaultValue);
        try {
            registry.register(flag);
            return flag;
        } catch (FlagConflictException exception) {
            return existingFlag(registry, name, exception);
        } catch (IllegalStateException exception) {
            // WorldGuard locks the registry once it is enabled (e.g. when the
            // plugin is hot-reloaded through a plugin manager). Reuse the flag
            // that was registered during the previous load if possible.
            return existingFlag(registry, name, exception);
        }
    }

    private static StateFlag existingFlag(FlagRegistry registry, String name, RuntimeException cause) {
        Flag<?> existing = registry.get(name);
        if (existing instanceof StateFlag stateFlag) {
            return stateFlag;
        }
        throw new IllegalStateException("WorldGuard flag '" + name
                + "' could not be registered. It either exists with an incompatible type"
                + " or the WorldGuard flag registry is already locked.", cause);
    }
}
