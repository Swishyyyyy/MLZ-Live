package com.mlz.boatlag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapEqualizerTest {

    /** 30 blocks per second, driving along +x. */
    private static final Point SPEED = new Point(0.03, 0, 0);
    private static final Point FAR_AWAY = new Point(100, 0, 100);

    /** Vanilla snap is about speed x ping: where the game has you vs where the server stopped you. */
    private static Point riderAfter(Point target, long ping) {
        return target.plus(SPEED.times(ping));
    }

    @Test
    void lowPingIsPlainVanilla() {
        Point target = new Point(0, 0, 0);
        Point result = SnapEqualizer.equalise(riderAfter(target, 40), target, SPEED, 40, 50, 6, FAR_AWAY, 1.45);
        assertEquals(target, result);
    }

    @Test
    void highPingGetsTheSameSnapAsReferencePing() {
        Point target = new Point(0, 0, 0);
        Point rider = riderAfter(target, 150);
        Point result = SnapEqualizer.equalise(rider, target, SPEED, 150, 50, 6, FAR_AWAY, 1.45);

        double referenceSnap = SPEED.length() * 50;          // 1.5 blocks
        assertEquals(referenceSnap, rider.distanceTo(result), 1e-9);
    }

    @Test
    void neverMovesPastWhereTheGameHasYou() {
        Point target = new Point(0, 0, 0);
        Point rider = new Point(0.5, 0, 0);                  // game is only 0.5 ahead
        Point result = SnapEqualizer.equalise(rider, target, SPEED, 200, 50, 6, FAR_AWAY, 1.45);
        assertTrue(result.x() <= rider.x() + 1e-9);
    }

    @Test
    void neverMovedIntoOrThroughTheOtherBoat() {
        Point target = new Point(0, 0, 0);
        Point other = new Point(2.0, 0, 0);                  // other boat 2 blocks ahead
        Point rider = riderAfter(target, 200);               // game is 6 blocks ahead, past it
        Point result = SnapEqualizer.equalise(rider, target, SPEED, 200, 50, 6, other, 1.45);

        assertTrue(SnapEqualizer.boxGap(result, other) >= 1.45 - 1e-9);
        assertTrue(result.x() < other.x());                  // still behind it
    }

    @Test
    void alreadyTouchingTheBoatAheadMeansVanilla() {
        Point target = new Point(0, 0, 0);
        Point other = new Point(1.4, 0, 0);
        Point result = SnapEqualizer.equalise(riderAfter(target, 150), target, SPEED, 150, 50, 6, other, 1.45);
        assertEquals(target, result);
    }

    @Test
    void sideBumpCanStillMoveForward() {
        Point target = new Point(0, 0, 0);
        Point other = new Point(0, 0, 1.4);                  // boat right beside you
        Point rider = riderAfter(target, 150);
        Point result = SnapEqualizer.equalise(rider, target, SPEED, 150, 50, 6, other, 1.45);
        assertEquals(1.5, rider.distanceTo(result), 1e-9);
    }

    @Test
    void cappedShift() {
        Point target = new Point(0, 0, 0);
        Point rider = riderAfter(target, 300);
        Point result = SnapEqualizer.equalise(rider, target, SPEED, 300, 0, 2.0, FAR_AWAY, 1.45);
        assertEquals(2.0, target.distanceTo(result), 1e-9);
    }

    @Test
    void standingStillDoesNothing() {
        Point target = new Point(0, 0, 0);
        Point result = SnapEqualizer.equalise(new Point(1, 0, 0), target, Point.ZERO, 200, 50, 6, FAR_AWAY, 1.45);
        assertEquals(target, result);
    }
}
