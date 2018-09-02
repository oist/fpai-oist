/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.solarposition;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jp.oist.unit.ios.solarsystemlib.atomsphere.Atmosphere;
import jp.oist.unit.ios.solarsystemlib.ClearSky;
import jp.oist.unit.ios.solarsystemlib.Location;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;
import jp.oist.unit.ios.solarsystemlib.common.Consts;

public class SolarPosition {

    public static class Variable {
        private ZonedDateTime dateTime;
        private double apparentElevation;
        private double apparentZenith;
        private double azimuth;
        private double elevation;
        private double equationOfTime;
        private double zenith;
        private Location location = null;

        public Variable(ZonedDateTime dateTime, double apparentElevation, double apparentZenith,
                        double azimuth, double elevation, double equationOfTime, double zenith) {
            this(dateTime, apparentElevation, apparentZenith, azimuth, elevation,
                 equationOfTime, zenith, null);
        }

        public Variable(ZonedDateTime dateTime, double apparentElevation, double apparentZenith,
                        double azimuth, double elevation, double equationOfTime, double zenith,
                        Location location) {
            this.dateTime = dateTime;
            this.apparentElevation = apparentElevation;
            this.apparentZenith = apparentZenith;
            this.azimuth = azimuth;
            this.elevation = elevation;
            this.equationOfTime = equationOfTime;
            this.zenith = zenith;
            this.location = location;
        }

        public ZonedDateTime getDateTime() { return dateTime; }
        public double getApparentElevation() { return apparentElevation; }
        public double getApparentZenith() { return apparentZenith; }
        public double getAzimuth() { return azimuth; }
        public double getElevation() { return elevation; }
        public double getEquationOfTime() { return equationOfTime; }
        public double getZenith() { return zenith; }
        public Location getLocation() { return location; }
        void setLocation(Location location) { this.location = location; }

        public String toString() {
            return String.format("{ \n"
                + "\ttimestamp: %s,\n"
                + "\tapparent_elevation : %.6f,\n"
                + "\tapparent_zenith : %.6f,\n"
                + "\tazimuth : %.6f,\n"
                + "\televation : %.6f,\n"
                + "\tequation_of_time : %.6f,\n"
                + "\tzenith : %.6f\n"
                + "}",
                dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                apparentElevation,
                apparentZenith,
                azimuth,
                elevation,
                equationOfTime,
                zenith);
        }
    }

    public static double dayAngle(ZonedDateTime dt) {
        return dayAngle(dt.getDayOfYear());
    }

    public static double dayAngle(int dayOfYear) {
        return ((2.0 * Math.PI / 365.0) * (dayOfYear - 1));
    }

    private ModelCollection factory;

    public SolarPosition(ModelCollection factory) {
        this.factory = factory;
    }
    /**
     *  Calculate the clear sky estimates of GHI, DNI, and/or DHI
     *   at this location.
     *   by simplified Solis
     * @return
     */
    public Irradiance.Variable getClearSky(SolarPosition.Variable var) {
        Atmosphere atmp = factory.atmosphere();
        double press = Consts.DEFAULT_PRESSURE;
        if (var.location != null)
            press = atmp.alt2pres(var.location.altitude);
        return getClearSky(var, press);
    }

    /**
     *  Calculate the clear sky estimates of GHI, DNI, and/or DHI
     *   at this location.
     *   by simplified Solis
     * @param pressure
     * @return
     */
    public Irradiance.Variable getClearSky(SolarPosition.Variable var, double pressure) {
        Irradiance irradiance = factory.irradiance();

        int doy = var.dateTime.getDayOfYear();
        double dniExtra = irradiance.extraRadiation(doy);

        Irradiance.Variable irradVars = null;
        if(var.location != null)
            irradVars = ClearSky.simplified_solis(var.apparentElevation, pressure, dniExtra);

        return irradVars;
    }

    /**
     * @param dt datetime on place
     * @param loc location of place
     * @return
     */
    public SolarPosition.Variable estimate(ZonedDateTime dt, Location loc) {
        return estimate(dt, loc, null, null, null, null);
    }

    public SolarPosition.Variable estimate(ZonedDateTime dt, Location loc,
                                           Double pressure, Double tempAir) {
        return estimate(dt, loc, pressure, tempAir, null);
    }

    public SolarPosition.Variable estimate(ZonedDateTime dt, Location loc,
                                           Double pressure, Double tempAir,
                                           Double atmosRefract) {
        return estimate(dt, loc, pressure, tempAir, atmosRefract,67.0);
    }

    public SolarPosition.Variable estimate(ZonedDateTime dt, Location loc,
                                           Double pressure, Double tempAir,
                                           Double atmosRefract, Double deltaT) {
        SolarPositionModel model = factory.getSolarPositionModel();
        return model.estimate(dt, loc, pressure, tempAir, atmosRefract, deltaT, loc);
    }
}
