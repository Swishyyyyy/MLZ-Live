package com.mlz.boatlag;

/**
 * A spot (or a direction) in the world: just x, y, z.
 * We use our own tiny class instead of Bukkit's Location so the math
 * can be tested without a running server.
 */
public record Point(double x, double y, double z) {

    public static final Point ZERO = new Point(0, 0, 0);

    public Point plus(Point other) {
        return new Point(x + other.x, y + other.y, z + other.z);
    }

    public Point minus(Point other) {
        return new Point(x - other.x, y - other.y, z - other.z);
    }

    public Point times(double amount) {
        return new Point(x * amount, y * amount, z * amount);
    }

    public double distanceTo(Point other) {
        return other.minus(this).length();
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** Dot product: how much of this direction points along {@code other}. */
    public double dot(Point other) {
        return x * other.x + y * other.y + z * other.z;
    }

    /** Same point, but flat (y = 0). Boats race on flat ground, so we mostly ignore height. */
    public Point flat() {
        return new Point(x, 0, z);
    }

    /** This direction shrunk/stretched to exactly 1 block long. ZERO if it has no length. */
    public Point normalized() {
        double len = length();
        return len < 1e-9 ? ZERO : times(1.0 / len);
    }

    public Point withY(double newY) {
        return new Point(x, newY, z);
    }
}
