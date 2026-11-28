package com.mlz.boatlag;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import org.bukkit.util.Vector;

/**
 * Reads and writes the x/y/z inside a "vehicle move" packet.
 *
 * Minecraft changed how this packet stores its position between versions
 * (separate x, y, z numbers in older versions, a single Vec3 in newer ones),
 * so we try the newer layout first and fall back to the older one.
 */
final class PacketPositions {

    private PacketPositions() {
    }

    static Point read(PacketContainer packet) {
        StructureModifier<Vector> vectors = packet.getVectors();
        if (vectors.size() > 0) {
            Vector v = vectors.read(0);
            return v == null ? null : new Point(v.getX(), v.getY(), v.getZ());
        }

        StructureModifier<Double> numbers = packet.getDoubles();
        if (numbers.size() >= 3) {
            return new Point(numbers.read(0), numbers.read(1), numbers.read(2));
        }

        return null;
    }

    static void write(PacketContainer packet, Point point) {
        StructureModifier<Vector> vectors = packet.getVectors();
        if (vectors.size() > 0) {
            vectors.write(0, new Vector(point.x(), point.y(), point.z()));
            return;
        }

        StructureModifier<Double> numbers = packet.getDoubles();
        numbers.write(0, point.x());
        numbers.write(1, point.y());
        numbers.write(2, point.z());
    }
}
