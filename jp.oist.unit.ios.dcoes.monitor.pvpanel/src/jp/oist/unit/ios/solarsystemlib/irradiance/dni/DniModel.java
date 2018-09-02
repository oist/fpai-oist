/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.dni;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;

import java.time.ZonedDateTime;

public abstract class DniModel {

    protected ModelCollection factory;

    public DniModel(ModelCollection factory) {
        this.factory = factory;
    }

    /**
     *
     * @param dt DateTime on Place.
     * @param ghi GHI (Global Horizontal Irradiance) in Watt/m^2
     * @param zenith Solar zenith angle
     * @param vars extra variable for model calculation
     * @see DniErbsModel
     */
    public abstract Irradiance.Variable estimate(ZonedDateTime dt, double ghi, double zenith, Object... vars);
}