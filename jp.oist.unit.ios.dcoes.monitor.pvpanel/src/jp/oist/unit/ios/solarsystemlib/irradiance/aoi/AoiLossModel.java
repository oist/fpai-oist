/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.aoi;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public abstract class AoiLossModel {

    public static class NoLoss extends AoiLossModel {
        public NoLoss(ModelCollection factory) {
            super(factory);
        }

        @Override
        public double estimate(double aoi, Object... vars) {
            return 1.0;
        }
    }

    protected ModelCollection factory;

    public AoiLossModel(ModelCollection factory) {
        this.factory = factory;
    }

    public abstract double estimate(double aoi, Object... vars);
}