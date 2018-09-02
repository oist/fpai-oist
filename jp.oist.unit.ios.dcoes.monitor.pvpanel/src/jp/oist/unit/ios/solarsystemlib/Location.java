/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib;

import java.time.ZonedDateTime;

import jp.oist.unit.ios.solarsystemlib.atomsphere.Atmosphere;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.collection.DefaultModelCollection;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;

public class Location {
    public final double latitude;
    public final double longitude;
    public final Double altitude;

    private ModelCollection factory = new DefaultModelCollection();

    public Location(double latitude , double longitude) {
        this(latitude, longitude, null);
    }

    public Location(double latitude , double longitude , Double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public void setModelFactory(ModelCollection factory) {
        this.factory = factory;
    }

    /**
     *  calculate the solar zenith, azimuth, etc. at this location.
     * @param time
     * @return
     */
    public SolarPosition.Variable getSolarPosition(ZonedDateTime time){
        Double press = null;
        if (altitude != null)
            press = Atmosphere.alt2pres(altitude);

        return getSolarPosition(time, press, null);
    }

    public SolarPosition.Variable getSolarPosition(ZonedDateTime time, Double pressure, Double tempAir) {
        return getSolarPosition(time, pressure, tempAir, null, null);
    }

    /**
     * calculate the solar zenith, azimuth, etc. at this location.
     * @param dt
     * @param pressure air-pressure in pascal
     * @param tempAir
     * @param atmosRefract
     * @return
     */
    public SolarPosition.Variable getSolarPosition(ZonedDateTime dt, Double pressure,
                                                   Double tempAir, Double atmosRefract, Double deltaT){
        if (deltaT == null)
            deltaT = 67.0;

        SolarPosition sp = factory.solarposition();
        return sp.estimate(dt, this, pressure, tempAir, atmosRefract,deltaT);
    }

    public Airmass getAirmass(SolarPosition.Variable sp) {
        Atmosphere atmosphere = factory.atmosphere();
        return atmosphere.getAirmass(sp, altitude);
    }
}
