/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.skydiffuse;

import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;

public abstract class SkyDiffuseModel {

    private ModelCollection factory;

    public SkyDiffuseModel(ModelCollection factory) {
        this.factory = factory;
    }

    public abstract Double estimate(double surfaceTilt, double surfaceAzimuth,
                                    SolarPosition.Variable sp, Irradiance.Variable irrad,
                                    Airmass am, Object... vars);
}
