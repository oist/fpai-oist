/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.atomsphere.airmass;

import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public abstract class RelativeAirmassModel {

    protected ModelCollection factory;

    public RelativeAirmassModel(ModelCollection factory) {
        this.factory = factory;
    }

    public abstract Double estimate(SolarPosition.Variable solarPosition);
}
