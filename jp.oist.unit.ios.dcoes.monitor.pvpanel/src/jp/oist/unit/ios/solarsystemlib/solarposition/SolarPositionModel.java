/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.solarposition;

import jp.oist.unit.ios.solarsystemlib.Location;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

import java.time.ZonedDateTime;

public abstract class SolarPositionModel {

    protected ModelCollection factory;

    public SolarPositionModel(ModelCollection factory) {
        this.factory = factory;
    }

    public abstract SolarPosition.Variable estimate(ZonedDateTime dt, Location location,
                                                    Double pressure, Double tempAir, Object... vars);
}
