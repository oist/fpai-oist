/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.atomsphere.airmass;

public class Airmass {
    private final double relative;
    private final double absolute;

    public Airmass(double relative, double absolute) {
        this.relative = relative;
        this.absolute = absolute;
    }

    public double getRelativeAirmass() { return relative; }
    public double getAbsoluteAirmass() { return absolute; }
}
