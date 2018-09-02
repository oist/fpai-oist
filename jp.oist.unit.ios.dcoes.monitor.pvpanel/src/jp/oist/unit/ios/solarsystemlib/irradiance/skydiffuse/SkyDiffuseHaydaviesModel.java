/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.skydiffuse;

import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;

public class SkyDiffuseHaydaviesModel extends SkyDiffuseModel {

    public SkyDiffuseHaydaviesModel(ModelCollection factory) {
        super(factory);
    }

    @Override
    public Double estimate(double surfaceTilt, double surfaceAzimuth,
                           SolarPosition.Variable sp, Irradiance.Variable irrad, Airmass am,
                           Object... vars) {
        // projectionRatio
        Double rb = (vars.length > 0)? (Double)vars[0] : null;
        if (rb == null) {
            double cosTt = Irradiance.projection(surfaceTilt, surfaceAzimuth,
                                                 sp.getApparentZenith(), sp.getAzimuth());
            double cosSolarZenith = Math.cos(Math.toRadians(sp.getApparentZenith()));
            rb = cosTt / cosSolarZenith;
        }
        // anisotropy index
        double ai = irrad.dni / irrad.dniExtra;
        double term1 = 1 - ai;
        double term2 = 0.5 * (1 + Math.cos(Math.toRadians(surfaceTilt)));

        double skyDiffuse = irrad.dhi * (ai * rb  + term1 * term2);
        return Math.max(skyDiffuse, 0.0);
    }
}
