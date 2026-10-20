package de.jugglegaming.oneWayElytra.utils;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class ActionBar {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private ActionBar() {
    }

    public static void send(Player player, String text) {
        player.sendActionBar(LEGACY_SERIALIZER.deserialize(text == null ? "" : text));
    }
}
