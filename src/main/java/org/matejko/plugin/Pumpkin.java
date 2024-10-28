package main.java.org.matejko.plugin;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class Pumpkin extends JavaPlugin implements Listener {

    private Set<String> changedBlocks; // Set to track changed blocks
    private boolean notificationsEnabled; // To track if notifications are enabled from config
    private File dataFile; // File to persist changed block locations
    private File configFile; // Configuration file
    private Logger logger; // Custom logger for the plugin

    @Override
    public void onEnable() {
        // Initialize the logger with a custom name
        this.logger = Logger.getLogger("Pumpkins");

        // Initialize the changedBlocks set
        this.changedBlocks = new HashSet<>();

        // Create the plugin data folder "PumpkinPower" if it doesn't exist
        File pumpkinPowerDir = new File(getDataFolder().getParentFile(), "PumpkinPower");
        if (!pumpkinPowerDir.exists()) {
            pumpkinPowerDir.mkdirs(); // Create the "PumpkinPower" folder if it doesn't exist
        }

        // Set the paths for dataFile and configFile inside the "PumpkinPower" directory
        this.dataFile = new File(pumpkinPowerDir, "changed_blocks.txt");
        this.configFile = new File(pumpkinPowerDir, "config.yml");

        // Create the configuration file if it doesn't exist
        if (!configFile.exists()) {
            saveDefaultConfig(); // Create the default config file if missing
        }

        // Load the plugin configuration (for logging notifications)
        loadConfig();

        // Ensure the changed blocks file exists
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile(); // Create the file if it doesn't exist
            } catch (IOException e) {
                this.logger.warning("Failed to create changed_blocks.txt: " + e.getMessage());
            }
        }

        // Load the saved blocks from file
        loadChangedBlocks();

        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);
        this.logger.info("PumpkinPowerPlugin enabled!");
    }

    @Override
    public void onDisable() {
        // Save the changed blocks to the file when disabling the plugin
        saveChangedBlocks();
        this.logger.info("PumpkinPowerPlugin disabled.");
    }

    // Load configuration manually
    private void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("notifications-enabled")) {
                    String value = line.split(":")[1].trim();
                    notificationsEnabled = Boolean.parseBoolean(value);  // Parse the value directly
                }
            }
        } catch (IOException e) {
            this.logger.warning("Failed to load config.yml: " + e.getMessage());
            // Default to true if the config cannot be read
            notificationsEnabled = true;
        }
    }

    // Save default config if it doesn't exist
    private void saveDefaultConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write("notifications-enabled: true\n");
        } catch (IOException e) {
            this.logger.warning("Failed to save default config.yml: " + e.getMessage());
        }
    }

    // Load the changed blocks from the file
    private void loadChangedBlocks() {
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                changedBlocks.add(line.trim());
            }
        } catch (IOException e) {
            this.logger.warning("Failed to load changed blocks from file: " + e.getMessage());
        }
    }

    // Save the changed blocks to the file
    private void saveChangedBlocks() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile))) {
            for (String location : changedBlocks) {
                writer.write(location);
                writer.newLine();
            }
        } catch (IOException e) {
            this.logger.warning("Failed to save changed blocks to file: " + e.getMessage());
        }
    }

    @EventHandler
    public void onBlockRedstonePower(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        String location = block.getLocation().toString();

        // Check if the block is Pumpkin (ID 86)
        if (block.getTypeId() == 86) {
            int powerLevel = block.getBlockPower();

            // If powered and not already Jack o' Lantern
            if (powerLevel > 0 && !changedBlocks.contains(location)) {
                block.setTypeId(91); // Change to Jack o' Lantern
                changedBlocks.add(location);

                // Send notification if enabled
                if (notificationsEnabled) {
                    logger.info("Pumpkin powered, changing to Jack o' Lantern at " + block.getLocation());
                }
            }
            // If unpowered and already Jack o' Lantern
            else if (powerLevel == 0 && changedBlocks.contains(location)) {
                block.setTypeId(86); // Revert to Pumpkin
                changedBlocks.remove(location);

                // Send notification if enabled
                if (notificationsEnabled) {
                    logger.info("Jack o' Lantern unpowered, reverting to Pumpkin at " + block.getLocation());
                }
            }
        }
        // Ensure Jack o' Lantern is reverted if power is off (only if the block was previously changed)
        else if (block.getTypeId() == 91 && changedBlocks.contains(location)) {
            int powerLevel = block.getBlockPower();
            
            if (powerLevel == 0) {  // If unpowered
                block.setTypeId(86); // Revert to Pumpkin
                changedBlocks.remove(location);

                // Send notification if enabled
                if (notificationsEnabled) {
                    logger.info("Jack o' Lantern unpowered, reverting to Pumpkin at " + block.getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // No longer preventing any block from being broken
        Block block = event.getBlock();
        String blockLocation = block.getLocation().toString();

        // The break event will no longer be cancelled for Jack o' Lantern blocks in the changed blocks list.
        // If you still want to log this, you can do it here:
        if (notificationsEnabled && changedBlocks.contains(blockLocation)) {
            logger.info("Block at " + block.getLocation() + " is being broken (previously changed).");
        }
    }
}
