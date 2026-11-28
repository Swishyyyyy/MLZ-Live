package com.mlz.boatlag;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Once per tick, for every rider, notes the closest other boat and where the server has it.
 *
 * That tells the packet code two things:
 *   - this correction is a boat hit (not a wall or a /tp), so it's boat lag
 *   - where the other boat is, so we never move anyone into it
 *
 * Runs on the main server thread (looking at entities isn't safe anywhere else).
 * The packet code reads the results from a thread-safe map.
 */
public final class ContactTracker {

    /**
     * @param otherBoat     where the server has the closest other boat
     * @param takenAtMillis when this was noted
     */
    public record Contact(Point otherBoat, long takenAtMillis) {
    }

    private final MLZBoatLag plugin;
    private final Map<UUID, Contact> contacts = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ContactTracker(MLZBoatLag plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkEveryRider, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        contacts.clear();
    }

    private void checkEveryRider() {
        BoatLagSettings settings = plugin.settings();
        if (!settings.enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        double radius = settings.contactRadiusBlocks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getVehicle() instanceof Boat myBoat)) {
                continue;
            }
            if (!settings.appliesInWorld(myBoat.getWorld().getName())) {
                continue;
            }
            Point me = toPoint(myBoat.getLocation());

            Point closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (Entity nearby : myBoat.getNearbyEntities(radius, 1.5, radius)) {
                if (nearby instanceof Boat otherBoat) {
                    Point there = toPoint(otherBoat.getLocation());
                    double distance = there.distanceTo(me);
                    if (distance < closestDistance) {
                        closest = there;
                        closestDistance = distance;
                    }
                }
            }
            if (closest != null) {
                contacts.put(player.getUniqueId(), new Contact(closest, now));
            }
        }
    }

    /** The latest note for this rider, or null if they haven't been near a boat recently. Safe from any thread. */
    public Contact recentContact(UUID rider, BoatLagSettings settings) {
        Contact contact = contacts.get(rider);
        if (contact == null) {
            return null;
        }
        boolean recent = System.currentTimeMillis() - contact.takenAtMillis() <= settings.contactMemoryMillis();
        return recent ? contact : null;
    }

    public void forget(UUID rider) {
        contacts.remove(rider);
    }

    private static Point toPoint(Location location) {
        return new Point(location.getX(), location.getY(), location.getZ());
    }
}
