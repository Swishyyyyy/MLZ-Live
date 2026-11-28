package com.mlz.boatlag;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything from config.yml, read once and stored as plain values.
 * A fresh copy is made on /boatlag reload.
 */
public record BoatLagSettings(
        boolean enabled,
        long referencePingMillis,
        long maxPingMillis,
        double maxShiftBlocks,
        double clearanceBlocks,
        double teleportThresholdBlocks,
        double contactRadiusBlocks,
        long contactMemoryMillis,
        Set<String> worlds) {

    public static BoatLagSettings fromConfig(FileConfiguration config) {
        Set<String> worlds = config.getStringList("worlds").stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        long reference = Math.max(0, config.getLong("reference-ping-millis", 50));
        return new BoatLagSettings(
                config.getBoolean("enabled", true),
                reference,
                Math.max(reference, config.getLong("max-ping-millis", 300)),
                Math.max(0, config.getDouble("max-shift-blocks", 6.0)),
                Math.max(1.375, config.getDouble("clearance-blocks", 1.45)),
                Math.max(1.0, config.getDouble("teleport-threshold-blocks", 12.0)),
                Math.max(0.5, config.getDouble("contact-radius-blocks", 3.0)),
                Math.max(100, config.getLong("contact-memory-millis", 1000)),
                worlds);
    }

    public boolean appliesInWorld(String worldName) {
        return worlds.isEmpty() || worlds.contains(worldName.toLowerCase(Locale.ROOT));
    }
}
