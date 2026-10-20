package de.jugglegaming.oneWayElytra.listeners;

import de.jugglegaming.oneWayElytra.OneWayElytra;
import org.bukkit.event.Listener;

/**
 * Reserved for item-specific listeners. Flight handling for tagged elytra
 * items lives in {@link OneWayElytraListener}.
 */
public class ElytraItemListener implements Listener {

    private final OneWayElytra oneWayElytra;

    public ElytraItemListener(OneWayElytra oneWayElytra) {
        this.oneWayElytra = oneWayElytra;
    }
}
