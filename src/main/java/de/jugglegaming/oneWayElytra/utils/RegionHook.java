package de.jugglegaming.oneWayElytra.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Region protection abstraction. Implemented by the optional WorldGuard hook
 * and deliberately free of any WorldGuard types, so the plugin can be loaded
 * when WorldGuard is not installed.
 */
public interface RegionHook {

    /**
     * @return whether the player may start a one-way elytra flight at their
     *         current position.
     */
    boolean canStart(Player player);

    /**
     * @return whether a flying player may move to the given location.
     */
    boolean isEntryAllowed(Player player, Location location);

    /**
     * @return whether a tagged elytra item is usable at the player's position.
     */
    boolean isItemAllowed(Player player);
}
