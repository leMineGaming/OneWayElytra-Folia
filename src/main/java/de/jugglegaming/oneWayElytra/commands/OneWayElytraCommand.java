package de.jugglegaming.oneWayElytra.commands;

import de.jugglegaming.oneWayElytra.OneWayElytra;
import de.jugglegaming.oneWayElytra.listeners.OneWayElytraListener;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public class OneWayElytraCommand implements CommandExecutor {

    private final OneWayElytra oneWayElytra;
    private final OneWayElytraListener oneWayElytraListener;
    private final NamespacedKey elytraKey;

    public OneWayElytraCommand(OneWayElytra oneWayElytra, OneWayElytraListener oneWayElytraListener) {
        this.oneWayElytra = oneWayElytra;
        this.oneWayElytraListener = oneWayElytraListener;
        this.elytraKey = new NamespacedKey(oneWayElytra, "onewayelytra-elytraitem");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendVersionInfo(sender);
            return true;
        }

        if (!player.hasPermission("onewayelytra.command")) {
            sendMessage(player, "commandDenied");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(oneWayElytra.prefix + "§7/onewayelytra location <add/delete> [name]");
            player.sendMessage(oneWayElytra.prefix + "§7/onewayelytra location set [name] radius [number]");
            player.sendMessage(oneWayElytra.prefix + "§7/onewayelytra location list");
            player.sendMessage(oneWayElytra.prefix + "§7/onewayelytra tag <add/remove/info>");
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("location")) {
            handleLocation(player, args);
        } else if (subCommand.equals("tag")) {
            handleTag(player, args);
        } else {
            sendMessage(player, "wrongArgs");
        }

        return true;
    }

    private void handleLocation(Player player, String[] args) {
        // Commands of players in different regions run on different threads on
        // Folia, so config reads and writes are serialised on the file manager.
        synchronized (oneWayElytra.getFileManager()) {
            if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
                listLocations(player);
            } else if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
                addLocation(player, args[2]);
            } else if (args.length == 3
                    && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("delete"))) {
                removeLocation(player, args[2]);
            } else if (args.length == 5
                    && args[1].equalsIgnoreCase("set")
                    && args[3].equalsIgnoreCase("radius")) {
                setRadius(player, args[2], args[4]);
            } else {
                sendMessage(player, "wrongArgs");
            }
        }
    }

    private void addLocation(Player player, String locationName) {
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return;
        }

        int radius = oneWayElytra.getFileManager().getConfig().contains("radius")
                ? oneWayElytra.getFileManager().getConfig().getInt("radius")
                : 16;
        oneWayElytra.getFileManager().editConfig(config -> {
            String path = "locations." + locationName;
            config.set(path + ".world", location.getWorld().getName());
            config.set(path + ".x", location.getX());
            config.set(path + ".y", location.getY());
            config.set(path + ".z", location.getZ());
            config.set(path + ".radius", radius);
        });
        reloadAreas();

        player.sendMessage(oneWayElytra.prefix + oneWayElytra.getTools().replaceVariables(
                oneWayElytra.getFileManager().getMessages().getString("locationSet"), locationName));
    }

    private void removeLocation(Player player, String locationName) {
        Section locations = oneWayElytra.getFileManager().getConfig().getSection("locations");
        if (locations == null || !locations.contains(locationName)) {
            sendMessage(player, "locationNotFound");
            return;
        }

        oneWayElytra.getFileManager().editConfig(config -> config.remove("locations." + locationName));
        reloadAreas();

        player.sendMessage(oneWayElytra.prefix + oneWayElytra.getTools().replaceVariables(
                oneWayElytra.getFileManager().getMessages().getString("locationRemoved"), locationName));
    }

    private void setRadius(Player player, String locationName, String radiusInput) {
        Section locations = oneWayElytra.getFileManager().getConfig().getSection("locations");
        if (locations == null || !locations.contains(locationName)) {
            sendMessage(player, "locationNotFound");
            return;
        }

        int radius;
        try {
            radius = Integer.parseInt(radiusInput);
        } catch (NumberFormatException exception) {
            sendMessage(player, "valueMustBeInt");
            return;
        }

        if (radius < 0) {
            sendMessage(player, "valueMustBeHigherOrEqual");
            return;
        }

        oneWayElytra.getFileManager().editConfig(config ->
                config.set("locations." + locationName + ".radius", radius));
        reloadAreas();

        player.sendMessage(oneWayElytra.prefix + oneWayElytra.getTools().replaceVariables(
                oneWayElytra.getFileManager().getMessages().getString("radiusSet"), radius, locationName));
    }

    private void listLocations(Player player) {
        Section locations = oneWayElytra.getFileManager().getConfig().getSection("locations");
        if (locations == null) {
            return;
        }

        YamlDocument config = oneWayElytra.getFileManager().getConfig();
        for (String locationName : locations.getRoutesAsStrings(false)) {
            String path = "locations." + locationName;

            String world = config.getString(path + ".world");
            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            double radius = config.getDouble(path + ".radius");

            player.sendMessage("§e" + locationName + " §7→ "
                    + oneWayElytra.getFileManager().getMessages().getString("world") + ": §a" + world
                    + " §7X: §a" + x + " §7Y: §a" + y + " §7Z: §a" + z
                    + " §7" + oneWayElytra.getFileManager().getMessages().getString("radius") + ": §a" + radius);
        }
    }

    private void handleTag(Player player, String[] args) {
        if (args.length != 2) {
            sendMessage(player, "wrongArgs");
            return;
        }

        if (args[1].equalsIgnoreCase("add")) {
            addTag(player);
        } else if (args[1].equalsIgnoreCase("remove")) {
            removeTag(player);
        } else if (args[1].equalsIgnoreCase("info")) {
            showTagInfo(player);
        } else {
            sendMessage(player, "wrongArgs");
        }
    }

    private void addTag(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType().isAir()) {
            player.sendMessage(oneWayElytra.prefix + "§cNo chestplate equipped.");
            return;
        }

        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) {
            player.sendMessage(oneWayElytra.prefix + "§cThis item cannot be tagged.");
            return;
        }

        meta.getPersistentDataContainer().set(elytraKey, PersistentDataType.BYTE, (byte) 1);
        chestplate.setItemMeta(meta);
        // Write the modified stack back so the change also persists on server
        // implementations that return a copy from getChestplate().
        player.getInventory().setChestplate(chestplate);
        player.sendMessage(oneWayElytra.prefix + "§aSuccessfully added the OneWayElytra tag.");
    }

    private void removeTag(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !chestplate.hasItemMeta()) {
            player.sendMessage(oneWayElytra.prefix + "§cNo tagged chestplate equipped.");
            return;
        }

        ItemMeta meta = chestplate.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(elytraKey, PersistentDataType.BYTE)) {
            player.sendMessage(oneWayElytra.prefix + "§cNo tagged chestplate equipped.");
            return;
        }

        container.remove(elytraKey);
        chestplate.setItemMeta(meta);
        player.getInventory().setChestplate(chestplate);
        player.sendMessage(oneWayElytra.prefix + "§aSuccessfully removed the OneWayElytra tag.");
    }

    private void showTagInfo(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !chestplate.hasItemMeta()) {
            player.sendMessage(oneWayElytra.prefix + "§cNo chestplate equipped.");
            return;
        }

        PersistentDataContainer container = chestplate.getItemMeta().getPersistentDataContainer();
        player.sendMessage(oneWayElytra.prefix + "§7Tagged: §a"
                + container.has(elytraKey, PersistentDataType.BYTE));
    }

    private void reloadAreas() {
        oneWayElytra.getRadiusManager().loadAreas(oneWayElytra.getFileManager().getConfig());
        oneWayElytraListener.reloadSettings();
    }

    private void sendMessage(Player player, String messageKey) {
        String message = oneWayElytra.getFileManager().getMessages().getString(messageKey);
        player.sendMessage(oneWayElytra.prefix + oneWayElytra.getTools().replaceVariables(message));
    }

    private void sendVersionInfo(CommandSender sender) {
        sender.sendMessage(oneWayElytra.prefix + "*~*~*~*~*~*~*~* <<OneWayElytra>> *~*~*~*~*~*~*~*");
        sender.sendMessage(oneWayElytra.prefix + "Version: " + oneWayElytra.getPluginMeta().getVersion());
        sender.sendMessage(oneWayElytra.prefix + "Author: " + oneWayElytra.getPluginMeta().getAuthors());
        sender.sendMessage(oneWayElytra.prefix + "*~*~*~*~*~*~*~* <<OneWayElytra>> *~*~*~*~*~*~*~*");
    }
}
