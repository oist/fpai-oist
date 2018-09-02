/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.spectrum;

import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public abstract class SpectralLossModel {

    public static class NoLoss extends SpectralLossModel {

        public NoLoss(ModelCollection factory) {
            super(factory);
        }

        @Override
        public Double estimate(Airmass am, Object... vars) {
            return 1.0;
        }
    }

    protected ModelCollection factory;

    public SpectralLossModel(ModelCollection factory) {
        this.factory = factory;
    }

    public abstract Double estimate(Airmass am, Object... vars);
}