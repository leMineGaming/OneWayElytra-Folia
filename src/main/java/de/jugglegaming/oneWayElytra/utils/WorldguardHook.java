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

/**
 * WorldGuard implementation of {@link RegionHook}.
 *
 * <p>Only read-only queries are performed. Those are safe to call from Folia's
 * region threads (one thread per region, no shared mutable state).</p>
 */
public class WorldguardHook implements RegionHook {

    @Override
    public boolean canStart(Player player) {
        return testFlag(player, player.getLocation(), WorldguardFlags.OWE_START, false);
    }

    @Override
    public boolean isEntryAllowed(Player player, Location location) {
        return testFlag(player, location, WorldguardFlags.OWE_ENTRY, true);
    }

    @Override
    public boolean isItemAllowed(Player player) {
        return testFlag(player, player.getLocation(), WorldguardFlags.OWE_ITEM, true);
    }

    private boolean testFlag(Player player, Location location, com.sk89q.worldguard.protection.flags.StateFlag flag, boolean defaultValue) {
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
            // A failed query should never break flight handling.
            return defaultValue;
        }
    }
}
