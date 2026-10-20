package de.jugglegaming.oneWayElytra.listeners;

import de.jugglegaming.oneWayElytra.OneWayElytra;
import de.jugglegaming.oneWayElytra.utils.ActionBar;
import de.jugglegaming.oneWayElytra.utils.RegionHook;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Core flight logic.
 *
 * <p>All player state is accessed from the player's own region thread. The
 * repeating check is scheduled on the player's {@link ScheduledTask}-based
 * entity scheduler instead of the global Bukkit scheduler, which is required
 * on Folia where entities belong to region-bound threads.</p>
 */
public class OneWayElytraListener implements Listener {

    private static final long CHECK_PERIOD_TICKS = 3L;
    private static final long CHECK_INITIAL_DELAY_TICKS = 1L;

    private final OneWayElytra oneWayElytra;
    private final RegionHook regionHook;
    private final NamespacedKey elytraKey;

    /*
     * Player UUIDs instead of Player instances: region threads of different
     * players may access these sets concurrently.
     */
    private final Set<UUID> playersFlying = ConcurrentHashMap.newKeySet();
    private final Set<UUID> playersFalling = ConcurrentHashMap.newKeySet();
    private final Set<UUID> playersBoosted = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ScheduledTask> flightTasks = new ConcurrentHashMap<>();

    private volatile int boostMultiplier;
    private volatile boolean adventureEnabled;
    private volatile boolean shuttingDown;
    private ScheduledTask watchdogTask;

    public OneWayElytraListener(OneWayElytra oneWayElytra, RegionHook regionHook) {
        this.oneWayElytra = oneWayElytra;
        this.regionHook = regionHook;
        this.elytraKey = new NamespacedKey(oneWayElytra, "onewayelytra-elytraitem");
        reloadSettings();
    }

    /**
     * Re-reads cached, frequently used settings. Called on enable and after a
     * command changed the config.
     */
    public void reloadSettings() {
        this.boostMultiplier = oneWayElytra.getFileManager().getConfig().contains("boostMultiplier")
                ? oneWayElytra.getFileManager().getConfig().getInt("boostMultiplier")
                : 5;
        this.adventureEnabled = !oneWayElytra.getFileManager().getConfig().contains("adventure")
                || oneWayElytra.getFileManager().getConfig().getBoolean("adventure");
    }

    /**
     * Starts flight tracking for every player that is already online and
     * installs a lightweight watchdog on the global region scheduler.
     *
     * <p>The watchdog makes the plugin resilient against players whose entity
     * scheduler was replaced (for example after a respawn on a Folia build
     * where the respawn event fires before the new entity scheduler exists).
     * The player list is global state and may only be read from the global
     * region thread, which is exactly where this task runs.</p>
     */
    public void startTrackingAll() {
        for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
            track(player);
        }

        if (watchdogTask != null && watchdogTask.getExecutionState() == ScheduledTask.ExecutionState.RUNNING) {
            return;
        }

        watchdogTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(oneWayElytra, task -> {
            if (shuttingDown) {
                task.cancel();
                return;
            }
            for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
                track(player);
            }
        }, 100L, 100L);
    }

    /**
     * Schedules the repeating flight check on the player's entity scheduler.
     * The scheduler is retired automatically when the player leaves, and then
     * removes the task from the internal map.
     */
    private synchronized void track(Player player) {
        if (shuttingDown || !player.isOnline()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        ScheduledTask existing = flightTasks.get(playerId);
        if (existing != null && existing.getExecutionState() == ScheduledTask.ExecutionState.RUNNING) {
            return;
        }

        ScheduledTask[] holder = new ScheduledTask[1];
        holder[0] = player.getScheduler().runAtFixedRate(
                oneWayElytra,
                task -> tickSafely(player),
                () -> {
                    ScheduledTask scheduled = holder[0];
                    if (scheduled != null) {
                        flightTasks.remove(playerId, scheduled);
                    }
                },
                CHECK_INITIAL_DELAY_TICKS,
                CHECK_PERIOD_TICKS
        );

        if (holder[0] == null) {
            flightTasks.remove(playerId);
        } else {
            flightTasks.put(playerId, holder[0]);
        }
    }

    private synchronized void untrack(Player player) {
        UUID playerId = player.getUniqueId();
        playersFlying.remove(playerId);
        playersFalling.remove(playerId);
        playersBoosted.remove(playerId);
        ScheduledTask task = flightTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void tickSafely(Player player) {
        if (shuttingDown || !player.isOnline()) {
            return;
        }
        try {
            tick(player);
        } catch (RuntimeException exception) {
            // An exception would permanently cancel the repeating task on Folia.
            oneWayElytra.getLogger().log(Level.WARNING, "Failed to update the flight state of " + player.getName(), exception);
        }
    }

    private void tick(Player player) {
        UUID playerId = player.getUniqueId();
        GameMode gameMode = player.getGameMode();
        boolean flying = playersFlying.contains(playerId);
        boolean allowedToFly = isAllowedToFly(player);

        if (allowedToFly && !flying && player.isOnGround()) {
            if (gameMode == GameMode.SURVIVAL
                    || (gameMode == GameMode.ADVENTURE && adventureEnabled)) {
                player.setAllowFlight(true);
            }
        }

        if (!allowedToFly && !flying
                && (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE)) {
            player.setAllowFlight(false);
        }

        if ((gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE)
                && flying
                && !hasAirBelow(player)) {
            land(player);
        }
    }

    private void land(Player player) {
        UUID playerId = player.getUniqueId();
        // Remove the flying state first, otherwise the glide toggle below is
        // cancelled by onEntityGlide.
        playersFlying.remove(playerId);
        playersBoosted.remove(playerId);
        player.setGliding(false);

        player.getScheduler().runDelayed(oneWayElytra, task -> {
            if (!player.isOnline() || playersFlying.contains(playerId)) {
                return;
            }
            player.setAllowFlight(false);
            removeTaggedChestplate(player);
        }, null, 5L);
    }

    private boolean hasAirBelow(Player player) {
        return player.getLocation()
                .getBlock()
                .getRelative(BlockFace.DOWN)
                .getType()
                .isAir();
    }

    public boolean isAllowedToFly(Player player) {
        boolean hasTaggedElytra = hasTaggedElytra(player);
        if (hasTaggedElytra && regionHook != null && !regionHook.isItemAllowed(player)) {
            hasTaggedElytra = false;
        }

        boolean regionAllowsStart = regionHook != null && regionHook.canStart(player);
        boolean inRadiusArea = oneWayElytra.getRadiusManager().isInAnyArea(player.getLocation());

        return hasTaggedElytra || regionAllowsStart || inRadiusArea;
    }

    private boolean hasTaggedElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !chestplate.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer container = chestplate.getItemMeta().getPersistentDataContainer();
        return container.has(elytraKey, PersistentDataType.BYTE);
    }

    private void removeTaggedChestplate(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !hasTaggedElytra(player)) {
            return;
        }
        player.getInventory().setChestplate(null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        track(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        untrack(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        untrack(player);
        track(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null
                || (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        UUID playerId = player.getUniqueId();

        if (playersFlying.contains(playerId)
                && regionHook != null
                && !regionHook.isEntryAllowed(player, to)) {

            playersFlying.remove(playerId);
            playersFalling.add(playerId);
            playersBoosted.remove(playerId);

            player.setGliding(false);
            player.setFlying(false);
            player.setAllowFlight(false);

            player.setFallDistance(0f);
            player.setVelocity(new Vector(0, 0, 0));

            event.setTo(from);
            ActionBar.send(player, oneWayElytra.getTools().replaceVariables(
                    oneWayElytra.getFileManager().getMessages().getString("areaEntryNotAllowed")));
        }

        if (playersFalling.contains(playerId) && player.isOnGround()) {
            removeTaggedChestplate(player);
            playersFalling.remove(playerId);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        GameMode gameMode = player.getGameMode();

        if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.ADVENTURE) {
            return;
        }

        UUID playerId = player.getUniqueId();

        if (isAllowedToFly(player)) {
            event.setCancelled(true);
            player.setGliding(true);
            playersFlying.add(playerId);
            playersBoosted.remove(playerId);

            ActionBar.send(player, oneWayElytra.getTools().replaceVariables(
                    oneWayElytra.getFileManager().getMessages().getString("boostMessage")));

            player.getScheduler().runDelayed(oneWayElytra, task -> {
                if (player.isOnline()) {
                    player.setAllowFlight(false);
                }
            }, null, 1L);
        } else if (hasTaggedElytra(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        if (!playersFlying.contains(playerId) && !playersFalling.contains(playerId)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL
                || cause == EntityDamageEvent.DamageCause.FLY_INTO_WALL
                || cause == EntityDamageEvent.DamageCause.CONTACT) {
            event.setCancelled(true);
            player.setFallDistance(0f);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBoost(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (playersBoosted.contains(playerId) || !playersFlying.contains(playerId)) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            playersBoosted.add(playerId);
            player.setVelocity(player.getLocation().getDirection().multiply(boostMultiplier));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityGlide(EntityToggleGlideEvent event) {
        if (event.getEntityType() == EntityType.PLAYER
                && playersFlying.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo().getWorld();

        if (fromWorld == null || toWorld == null || fromWorld.equals(toWorld)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (playersFlying.contains(playerId)) {
            playersFlying.remove(playerId);
            playersBoosted.remove(playerId);
            player.setGliding(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (playersFlying.contains(player.getUniqueId()) && hasTaggedElytra(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!playersFlying.contains(player.getUniqueId())) {
            return;
        }

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            cancelIfTagged(event, event.getCurrentItem());
        }

        if (event.isShiftClick()) {
            cancelIfTagged(event, event.getCurrentItem());
        }
    }

    private void cancelIfTagged(InventoryClickEvent event, ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            if (container.has(elytraKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Cancels all scheduled checks and clears per-player state.
     */
    public void shutdown() {
        shuttingDown = true;
        if (watchdogTask != null) {
            watchdogTask.cancel();
            watchdogTask = null;
        }
        for (ScheduledTask task : flightTasks.values()) {
            task.cancel();
        }
        flightTasks.clear();
        playersFlying.clear();
        playersFalling.clear();
        playersBoosted.clear();
    }
}
