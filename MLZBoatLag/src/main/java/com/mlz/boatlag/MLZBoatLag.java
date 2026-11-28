package com.mlz.boatlag;

import com.comphenix.protocol.ProtocolLibrary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin entry point. Its only job is wiring the pieces together:
 *
 *   ContactTracker      -> who has been near another boat recently, and where it is
 *   BoatPacketListener  -> lets the vanilla snap-back through, evened out for ping
 *   SnapEqualizer       -> the actual rule: the snap is the size a reference-ping rider would get
 *   BoatLagCommand      -> /boatlag reload | debug
 */
public final class MLZBoatLag extends JavaPlugin implements Listener {

    private volatile BoatLagSettings settings;
    private ContactTracker contactTracker;
    private BoatPacketListener packetListener;
    private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        contactTracker = new ContactTracker(this);
        contactTracker.start();

        packetListener = new BoatPacketListener(this, contactTracker);
        ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);

        getServer().getPluginManager().registerEvents(this, this);

        BoatLagCommand command = new BoatLagCommand(this);
        PluginCommand boatlag = getCommand("boatlag");
        if (boatlag != null) {
            boatlag.setExecutor(command);
            boatlag.setTabCompleter(command);
        }

        getLogger().info("Vanilla boat lag, snap-back matched to " + settings.referencePingMillis() + " ms ping.");
    }

    @Override
    public void onDisable() {
        if (packetListener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);
        }
        if (contactTracker != null) {
            contactTracker.stop();
        }
    }

    public void reloadSettings() {
        reloadConfig();
        settings = BoatLagSettings.fromConfig(getConfig());
    }

    public BoatLagSettings settings() {
        return settings;
    }

    // ---- debug readout ----

    public boolean isDebugging(UUID player) {
        return debugPlayers.contains(player);
    }

    /** Turns the readout on/off. Returns true if it's now on. */
    public boolean toggleDebug(UUID player) {
        if (debugPlayers.remove(player)) {
            return false;
        }
        debugPlayers.add(player);
        return true;
    }

    /** Packets arrive off the main thread, so hop back onto it before messaging. */
    public void sendActionBar(Player player, String message) {
        Bukkit.getScheduler().runTask(this, () ->
                player.sendActionBar(Component.text("[BoatLag] " + message, NamedTextColor.AQUA)));
    }

    // ---- cleanup ----

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        contactTracker.forget(id);
        packetListener.forget(id);
        debugPlayers.remove(id);
    }
}
