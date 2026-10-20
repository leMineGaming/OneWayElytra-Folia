package de.jugglegaming.oneWayElytra.managers;

import de.jugglegaming.oneWayElytra.OneWayElytra;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

public class FileManager {

    private final OneWayElytra oneWayElytra;

    private final ConsoleCommandSender ccs = Bukkit.getServer().getConsoleSender();

    private YamlDocument config;
    private YamlDocument messages;
    private YamlDocument database;

    public FileManager(OneWayElytra oneWayElytra) {
        this.oneWayElytra = oneWayElytra;
    }

    public void loadConfig(OneWayElytra oneWayElytra) {
        try {
            config = YamlDocument.create(new File(oneWayElytra.getDataFolder(), "config.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"), "config.yml resource is missing"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );
            config.update();
            config.save();
        } catch (IOException | RuntimeException exception) {
            oneWayElytra.getLogger().log(Level.SEVERE, "Could not load config.yml. Disabling the plugin...", exception);
            Bukkit.getPluginManager().disablePlugin(oneWayElytra);
        }
    }

    public void loadDatabase(OneWayElytra oneWayElytra) {
        try {
            database = YamlDocument.create(new File(oneWayElytra.getDataFolder(), "database.yml"),
                    Objects.requireNonNull(getClass().getResourceAsStream("/database.yml"), "database.yml resource is missing"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );
            database.update();
            database.save();
        } catch (IOException | RuntimeException exception) {
            oneWayElytra.getLogger().log(Level.SEVERE, "Could not load database.yml. Disabling the plugin...", exception);
            Bukkit.getPluginManager().disablePlugin(oneWayElytra);
        }
    }

    public void loadMessages(OneWayElytra oneWayElytra) {
        try {
            File languagesFolder = new File(oneWayElytra.getDataFolder(), "languages");
            if (!languagesFolder.exists() && !languagesFolder.mkdirs()) {
                oneWayElytra.getLogger().warning("Could not create the languages folder.");
            }

            String language = Objects.requireNonNullElse(config.getString("language"), "EN");
            InputStream defaults = getClass().getResourceAsStream("/" + language + ".yml");
            if (defaults == null) {
                oneWayElytra.getLogger().warning("Language '" + language + "' was not found inside the jar. Falling back to EN.");
                language = "EN";
                defaults = getClass().getResourceAsStream("/EN.yml");
            }

            messages = YamlDocument.create(new File(languagesFolder, language + ".yml"),
                    Objects.requireNonNull(defaults, "No language file is bundled with the plugin"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
            );
            messages.update();
            messages.save();
        } catch (IOException | RuntimeException exception) {
            oneWayElytra.getLogger().log(Level.SEVERE, "Could not load the messages file. Disabling the plugin...", exception);
            Bukkit.getPluginManager().disablePlugin(oneWayElytra);
        }
    }

    /**
     * Applies a config change and saves the file while holding the file lock.
     * Commands may run on different region threads on Folia, so config writes
     * must not interleave.
     */
    public synchronized void editConfig(Consumer<YamlDocument> editor) {
        if (config == null) {
            return;
        }
        editor.accept(config);
        try {
            config.save();
        } catch (IOException exception) {
            oneWayElytra.getLogger().log(Level.SEVERE, "Could not save config.yml!", exception);
            ccs.sendMessage("§cCould not save config.yml!");
        }
    }

    public synchronized void saveConfig() {
        if (config == null) {
            return;
        }
        try {
            config.save();
        } catch (IOException exception) {
            oneWayElytra.getLogger().log(Level.SEVERE, "Could not save config.yml!", exception);
            ccs.sendMessage("§cCould not save config.yml!");
        }
    }

    public YamlDocument getConfig() {
        return config;
    }

    public YamlDocument getMessages() {
        return messages;
    }

    public YamlDocument getDatabase() {
        return database;
    }
}
