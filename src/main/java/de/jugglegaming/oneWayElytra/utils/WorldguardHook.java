package de.jugglegaming.oneWayElytra.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WorldGuard implementation of {@link RegionHook}.
 *
 * <p>Only read-only queries are performed. WorldGuard's region query cache and
 * region indexes are thread-safe, so those queries are safe to call from
 * Folia's region threads (one thread per region, no shared mutable state).</p>
 */
public class WorldguardHook implements RegionHook {

    private static final Logger LOGGER = Logger.getLogger("OneWayElytra");

    private volatile boolean queryFailureLogged;

    @Override
    public boolean canStart(Player player) {
        return testFlag(player, player.getLocation(), WorldguardFlags.getStart(), false);
    }

    @Override
    public boolean isEntryAllowed(Player player, Location location) {
        return testFlag(player, location, WorldguardFlags.getEntry(), true);
    }

    @Override
    public boolean isItemAllowed(Player player) {
        return testFlag(player, player.getLocation(), WorldguardFlags.getItem(), true);
    }

    private boolean testFlag(Player player, Location location,
                             com.sk89q.worldguard.protection.flags.StateFlag flag,
                             boolean defaultValue) {
        if (flag == null || location == null || location.getWorld() == null) {
            return defaultValue;
        }

        WorldGuardPlugin worldGuardPlugin = WorldGuardPlugin.inst();
        if (worldGuardPlugin == null || !worldGuardPlugin.isEnabled()) {
            return defaultValue;
        }

        try {
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(loc);
            LocalPlayer localPlayer = worldGuardPlugin.wrapPlayer(player);
            return set.testState(localPlayer, flag);
        } catch (RuntimeException exception) {
            // A failed query should never break flight handling, but it must not
            // silently disable all region flags either. Log it once so the
            // configuration problem is visible instead of looking like a broken
            // hook.
            if (!queryFailureLogged) {
                queryFailureLogged = true;
                LOGGER.log(Level.WARNING, "WorldGuard region query failed; falling back to flag defaults. "
                        + "OneWayElytra's WorldGuard flags will not be applied until this is fixed.", exception);
            }
            return defaultValue;
        }
    }
}
