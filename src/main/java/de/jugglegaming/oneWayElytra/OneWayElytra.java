package de.jugglegaming.oneWayElytra;

import de.jugglegaming.oneWayElytra.commands.OneWayElytraCommand;
import de.jugglegaming.oneWayElytra.listeners.ElytraItemListener;
import de.jugglegaming.oneWayElytra.listeners.OneWayElytraListener;
import de.jugglegaming.oneWayElytra.managers.FileManager;
import de.jugglegaming.oneWayElytra.managers.RadiusManager;
import de.jugglegaming.oneWayElytra.utils.RegionHook;
import de.jugglegaming.oneWayElytra.utils.Tools;
import de.jugglegaming.oneWayElytra.utils.WorldguardFlags;
import de.jugglegaming.oneWayElytra.utils.WorldguardHook;
import de.jugglegaming.oneWayElytra.utils.WorldguardUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public final class OneWayElytra extends JavaPlugin {

    private static OneWayElytra instance;

    public String prefix = "§5OWE §f» ";

    private FileManager fileManager;
    private RadiusManager radiusManager;
    private Tools tools;

    private OneWayElytraListener oneWayElytraListener;
    private ElytraItemListener elytraItemListener;

    /**
     * Abstraction around the optional WorldGuard integration. The listener only
     * ever references this interface, so WorldGuard classes are not loaded when
     * WorldGuard is not installed.
     */
    private RegionHook regionHook;
    private boolean worldguardInstalled;

    private final PluginManager pluginManager = Bukkit.getPluginManager();
    private final ConsoleCommandSender console = Bukkit.getConsoleSender();

    @Override
    public void onLoad() {
        connectToWorldguard();
    }

    @Override
    public void onEnable() {
        instance = this;

        fileManager = new FileManager(this);
        fileManager.loadConfig(this);
        if (fileManager.getConfig() == null) {
            return;
        }
        console.sendMessage("[OneWayElytra] Config loaded.");

        radiusManager = new RadiusManager(fileManager.getConfig());

        fileManager.loadMessages(this);
        if (fileManager.getMessages() == null) {
            return;
        }
        console.sendMessage("[OneWayElytra] Language loaded: §7" + fileManager.getConfig().getString("language"));

        this.tools = new Tools(this);
        prefix = this.tools.replaceVariables(Objects.requireNonNullElse(
                fileManager.getMessages().getString("prefix"), "&5OWE &f»") + " ");

        console.sendMessage(prefix + "Enabling...");
        registerListeners();
        console.sendMessage(prefix + "Listeners successfully registered!");
        registerCommands();
        console.sendMessage(prefix + "Commands successfully registered!");

        if (worldguardInstalled) {
            console.sendMessage(prefix + "WorldGuard: connected");
        } else {
            console.sendMessage(prefix + "WorldGuard: not found");
        }

        console.sendMessage(prefix + "*~*~*~*~*~*~*~* <<OneWayElytra>> *~*~*~*~*~*~*~*");
        console.sendMessage(prefix + "Plugin successfully loaded!");
        console.sendMessage(prefix + "Version: " + getPluginMeta().getVersion());
        console.sendMessage(prefix + "Author: " + getPluginMeta().getAuthors());
        console.sendMessage(prefix + "*~*~*~*~*~*~*~* <<OneWayElytra>> *~*~*~*~*~*~*~*");
    }

    @Override
    public void onDisable() {
        if (oneWayElytraListener != null) {
            oneWayElytraListener.shutdown();
        }
        console.sendMessage(prefix + "*~*~*~*~*~*~*~* <<OneWayElytra>> *~*~*~*~*~*~*~*");
        console.sendMessage(prefix + "Plugin successfully disabled!");
        console.sendMessage(prefix + "*~*~*~*~*~*~*~* <<OneWayElytra>> *~*~*~*~*~*~*~*");
    }

    public static OneWayElytra getInstance() {
        return instance;
    }

    public Tools getTools() {
        return tools;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public RadiusManager getRadiusManager() {
        return radiusManager;
    }

    public RegionHook getRegionHook() {
        return regionHook;
    }

    public boolean isWorldguardInstalled() {
        return worldguardInstalled;
    }

    private void registerListeners() {
        oneWayElytraListener = new OneWayElytraListener(this, regionHook);
        pluginManager.registerEvents(oneWayElytraListener, this);
        elytraItemListener = new ElytraItemListener(this);
        pluginManager.registerEvents(elytraItemListener, this);

        // Start the per-player flight checks for players that are already online
        // (relevant for /reload-like plugin loads on non-Folia servers).
        oneWayElytraListener.startTrackingAll();
    }

    private void registerCommands() {
        PluginCommand command = Objects.requireNonNull(getCommand("onewayelytra"), "onewayelytra command is missing from plugin.yml");
        command.setExecutor(new OneWayElytraCommand(this, oneWayElytraListener));
    }

    private void connectToWorldguard() {
        worldguardInstalled = WorldguardUtils.isWorldGuardInstalled();
        if (!worldguardInstalled) {
            console.sendMessage("[OneWayElytra] Worldguard not found.");
            return;
        }

        try {
            // WorldGuard flags must be registered during onLoad.
            WorldguardFlags.load();
            regionHook = new WorldguardHook();
            console.sendMessage("[OneWayElytra] Successfully connected to WorldGuard!");
        } catch (Throwable throwable) {
            // A missing or incompatible WorldGuard build must never take the
            // whole plugin down; the plugin simply runs without region flags.
            regionHook = null;
            worldguardInstalled = false;
            getLogger().log(Level.WARNING, "WorldGuard was found but could not be hooked. Continuing without WorldGuard support.", throwable);
        }
    }
}
