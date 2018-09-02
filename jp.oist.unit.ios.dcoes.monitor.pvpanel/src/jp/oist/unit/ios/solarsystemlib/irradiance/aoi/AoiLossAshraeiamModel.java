/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.aoi;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public class AoiLossAshraeiamModel extends AoiLossModel {

    public AoiLossAshraeiamModel(ModelCollection models) {
        super(models);
    }

    @Override
    public double estimate(double aoi, Object... vars) {
        double b = 0.05;
        if (vars != null && vars.length > 0)
            b = (Double)vars[0];

        return (aoi >= 90)? 0.0 : 1.0 - b * ((1.0 / Math.cos(Math.toRadians(aoi)) - 1));
    }
}