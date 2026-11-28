package com.mlz.boatlag;

/**
 * Makes the boat lag snap-back the same size for everyone, whatever their ping.
 *
 * Why high ping gets snapped further:
 *   When you hit a boat, the server stops your boat (the lightning rod) and tells your
 *   game "you're back HERE". By the time that message reaches you, your game has kept
 *   driving for a whole ping round trip. So the snap is roughly speed x ping:
 *   at 30 blocks/s, 40 ms ping = 1.2 blocks, 150 ms ping = 4.5 blocks.
 *
 * The fix:
 *   We leave the snap completely vanilla, but move the spot it sends you to forward
 *   along your driving line by speed x (your ping - reference ping). Then a 150 ping
 *   rider gets the same size snap as a reference-ping rider. Riders at or below the
 *   reference ping are never touched.
 *
 * Two limits keep it safe:
 *   - The new spot never goes past where your game already has you (the snap can only shrink).
 *   - The new spot always stays clear of the other boat, so the server doesn't just
 *     hit you into it again on your next move.
 */
public final class SnapEqualizer {

    /** How finely we step back when the full move would land inside the other boat. */
    private static final double STEP_BLOCKS = 0.05;

    private SnapEqualizer() {
    }

    /**
     * @param riderPosition   where the rider's game says their boat is (latest report)
     * @param serverTarget    where the vanilla correction wants to put them
     * @param velocityPerMs   the rider's own speed, from their reports, in blocks per millisecond
     * @param pingMillis      the rider's ping (already capped)
     * @param referencePingMs the ping everyone's snap is matched to
     * @param maxShiftBlocks  never move the target further than this
     * @param otherBoat       where the server has the other boat (null if unknown)
     * @param clearanceBlocks how far the target must stay from the other boat, center to center (box gap)
     * @return the new target, or {@code serverTarget} unchanged if nothing should change
     */
    public static Point equalise(Point riderPosition, Point serverTarget, Point velocityPerMs,
                                 long pingMillis, long referencePingMs, double maxShiftBlocks,
                                 Point otherBoat, double clearanceBlocks) {
        long extraPing = pingMillis - referencePingMs;
        if (extraPing <= 0) {
            return serverTarget;
        }

        Point flatVelocity = velocityPerMs.flat();
        Point forward = flatVelocity.normalized();
        if (forward.equals(Point.ZERO)) {
            return serverTarget;
        }

        // The extra distance this rider's game drove because of their extra ping.
        double shift = Math.min(flatVelocity.length() * extraPing, maxShiftBlocks);

        // Never past where their game already has them: the snap can only get smaller.
        double roomAhead = riderPosition.minus(serverTarget).flat().dot(forward);
        shift = Math.min(shift, Math.max(0, roomAhead));

        if (shift <= 0) {
            return serverTarget;
        }
        if (otherBoat == null) {
            return serverTarget.plus(forward.times(shift));
        }

        // Stay clear of the other boat: walk forward and stop before getting too close.
        // Walking (instead of just checking the end spot) also stops us jumping THROUGH it.
        // If we already start closer than the clearance, we're never allowed to get closer.
        Point other = otherBoat.flat();
        double allowed = Math.min(clearanceBlocks, boxGap(serverTarget, other));
        double good = 0;
        for (double step = STEP_BLOCKS; ; step += STEP_BLOCKS) {
            double tryShift = Math.min(step, shift);
            Point candidate = serverTarget.plus(forward.times(tryShift));
            if (boxGap(candidate, other) < allowed - 1e-6) {
                break;
            }
            good = tryShift;
            if (tryShift >= shift) {
                break;
            }
        }
        return good <= 0 ? serverTarget : serverTarget.plus(forward.times(good));
    }

    /**
     * How far apart two boats are the way Minecraft checks it. Boat hitboxes are square
     * boxes lined up with the world, so they only touch if they're close on BOTH x and z.
     * Center to center, they're clear once either gap reaches 1.375 blocks.
     */
    static double boxGap(Point a, Point b) {
        return Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z()));
    }
}
