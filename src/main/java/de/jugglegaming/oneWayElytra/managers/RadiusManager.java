package de.jugglegaming.oneWayElytra.managers;

import de.jugglegaming.oneWayElytra.utils.RadiusArea;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class RadiusManager {

    /**
     * Replaced as a whole and never mutated in place, so region threads can
     * read it safely while a command reloads the areas.
     */
    private volatile List<RadiusArea> areas = List.of();

    public RadiusManager(YamlDocument config) {
        loadAreas(config);
    }

    public void loadAreas(YamlDocument config) {
        List<RadiusArea> loadedAreas = new ArrayList<>();
        Section locations = config.getSection("locations");

        if (locations == null) {
            areas = List.of();
            return;
        }

        for (String key : locations.getRoutesAsStrings(false)) {
            String path = "locations." + key;

            String world = config.getString(path + ".world");
            double radius = config.getDouble(path + ".radius");

            if (world == null || radius <= 0) {
                continue;
            }

            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");

            loadedAreas.add(new RadiusArea(key, world, x, y, z, radius));
        }

        areas = List.copyOf(loadedAreas);
    }

    public boolean isInAnyArea(Location location) {
        for (RadiusArea area : areas) {
            if (area.isInside(location)) {
                return true;
            }
        }
        return false;
    }
}
