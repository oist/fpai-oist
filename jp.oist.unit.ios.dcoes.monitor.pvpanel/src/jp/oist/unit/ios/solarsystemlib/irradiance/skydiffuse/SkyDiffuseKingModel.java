/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.skydiffuse;

import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;

public class SkyDiffuseKingModel extends SkyDiffuseModel {

    public SkyDiffuseKingModel(ModelCollection factory) {
        super(factory);
    }

    public static double cosd(double degree) {
        return Math.cos(Math.toRadians(degree));
    }

    @Override
    public Double estimate(double surfaceTilt, double surfaceAzimuth,
                           SolarPosition.Variable sp, Irradiance.Variable irrad,
                           Airmass am, Object... vars) {
        double skyDiffuse = (irrad.dhi * (1.0 + cosd(surfaceTilt)) * 0.5 + irrad.ghi
                                * (0.012 * sp.getApparentZenith() - 0.04)
                                * (1.0 - cosd(surfaceTilt)) * 0.5);

        return Math.max(skyDiffuse, 0.0);
    }
}
