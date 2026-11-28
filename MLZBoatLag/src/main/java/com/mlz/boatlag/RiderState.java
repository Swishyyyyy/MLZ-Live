package com.mlz.boatlag;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * What we keep track of for one rider while they're in a boat:
 * where their game says the boat is, and how fast it's going.
 * Packets arrive on network threads, so everything here is thread-safe.
 */
final class RiderState {

    /** Speed is measured over this much time, so one late packet doesn't make it jump. */
    static final long SPEED_WINDOW_MILLIS = 150;

    /** A jump bigger than this between two reports is a teleport, not driving. */
    private static final double TELEPORT_JUMP_BLOCKS = 4.0;

    private record Sample(long millis, Point position) {
    }

    private final Deque<Sample> samples = new ArrayDeque<>();

    /** Called every time the rider's game tells us where their boat is. */
    synchronized void reportPosition(Point position, long now) {
        Sample last = samples.peekLast();
        if (last != null && last.position().distanceTo(position) > TELEPORT_JUMP_BLOCKS) {
            samples.clear();
        }
        samples.addLast(new Sample(now, position));
        while (samples.size() > 2 && now - samples.peekFirst().millis() > SPEED_WINDOW_MILLIS) {
            samples.removeFirst();
        }
    }

    synchronized Point lastReportedPosition() {
        Sample last = samples.peekLast();
        return last == null ? null : last.position();
    }

    /** Blocks per millisecond, from the rider's own reports. ZERO if we can't tell yet. */
    synchronized Point velocity() {
        if (samples.size() < 2) {
            return Point.ZERO;
        }
        Sample first = samples.peekFirst();
        Sample last = samples.peekLast();
        long millis = last.millis() - first.millis();
        if (millis < 20) {
            return Point.ZERO;
        }
        return last.position().minus(first.position()).times(1.0 / millis);
    }
}
