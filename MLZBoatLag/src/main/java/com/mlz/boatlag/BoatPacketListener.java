package com.mlz.boatlag;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches the two boat-position packets:
 *
 *   Rider -> server  ("my boat is here")
 *       We remember the position, so we know their speed.
 *
 *   Server -> rider  ("no, your boat is HERE")  <- the boat lag snap-back
 *       This stays VANILLA. Nothing is cancelled. If the rider has more ping than the
 *       reference, we only move the spot forward so the snap is the same size it would
 *       be for a reference-ping rider. See {@link SnapEqualizer}.
 */
public final class BoatPacketListener extends PacketAdapter {

    private final MLZBoatLag boatLag;
    private final ContactTracker contacts;
    private final Map<UUID, RiderState> riders = new ConcurrentHashMap<>();

    public BoatPacketListener(MLZBoatLag plugin, ContactTracker contacts) {
        super(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.VEHICLE_MOVE,
                PacketType.Play.Server.VEHICLE_MOVE);
        this.boatLag = plugin;
        this.contacts = contacts;
    }

    /** Rider -> server: remember where their game says the boat is. */
    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.isPlayerTemporary()) {
            return;
        }
        Point reported = PacketPositions.read(event.getPacket());
        if (reported == null) {
            return;
        }
        riders.computeIfAbsent(event.getPlayer().getUniqueId(), id -> new RiderState())
                .reportPosition(reported, System.currentTimeMillis());
    }

    /** Server -> rider: the snap-back. Vanilla, with the size evened out for ping. */
    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.isPlayerTemporary()) {
            return;
        }

        BoatLagSettings settings = boatLag.settings();
        if (!settings.enabled()) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        // No boat nearby recently -> not boat lag (wall, teleport, etc). Leave it alone.
        ContactTracker.Contact contact = contacts.recentContact(id, settings);
        if (contact == null) {
            return;
        }

        RiderState rider = riders.get(id);
        Point riderPosition = rider == null ? null : rider.lastReportedPosition();
        if (riderPosition == null) {
            return;
        }

        Point serverTarget = PacketPositions.read(event.getPacket());
        if (serverTarget == null) {
            return;
        }

        double vanillaSnap = riderPosition.distanceTo(serverTarget);

        // A huge correction is a warp or /tp, not boat lag.
        if (vanillaSnap >= settings.teleportThresholdBlocks()) {
            return;
        }

        long ping = Math.max(0, Math.min(settings.maxPingMillis(), player.getPing()));

        Point equalised = SnapEqualizer.equalise(
                riderPosition, serverTarget, rider.velocity(),
                ping, settings.referencePingMillis(), settings.maxShiftBlocks(),
                contact.otherBoat(), settings.clearanceBlocks());

        if (equalised.equals(serverTarget)) {
            showDebug(player, String.format(Locale.ROOT,
                    "vanilla snap %.2f (ping %d, unchanged)", vanillaSnap, ping));
            return;
        }

        // Only the POSITION changes. The packet carries no speed, so momentum is untouched.
        // Copy the packet before changing it, in case the server reuses the original.
        PacketContainer copy = event.getPacket().shallowClone();
        PacketPositions.write(copy, equalised);
        event.setPacket(copy);

        showDebug(player, String.format(Locale.ROOT,
                "vanilla snap %.2f -> %.2f (ping %d, ref %d)",
                vanillaSnap, riderPosition.distanceTo(equalised), ping, settings.referencePingMillis()));
    }

    public void forget(UUID rider) {
        riders.remove(rider);
    }

    private void showDebug(Player player, String message) {
        if (boatLag.isDebugging(player.getUniqueId())) {
            boatLag.sendActionBar(player, message);
        }
    }
}
