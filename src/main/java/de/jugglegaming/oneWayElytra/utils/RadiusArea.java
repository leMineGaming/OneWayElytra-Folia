package de.jugglegaming.oneWayElytra.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class RadiusArea {

    private final String name;
    private final String world;
    private final double x, y, z;
    private final double radiusSquared;

    public RadiusArea(String name, String world, double x, double y, double z, double radius) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radiusSquared = radius * radius;
    }

    public String getName() {
        return name;
    }

    public boolean isInside(Location location) {
        if (location == null) {
            return false;
        }

        World locationWorld = location.getWorld();
        if (locationWorld == null || !locationWorld.getName().equals(world)) {
            return false;
        }

        double dx = location.getX() - x;
        double dy = location.getY() - y;
        double dz = location.getZ() - z;

        return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
    }
}
