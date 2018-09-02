/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.extraradiation;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public abstract class  ExtraRadiationModel {

    protected ModelCollection factory;

    public ExtraRadiationModel(ModelCollection factory) {
        this.factory = factory;
    }

    public abstract Double estimate(int doy, Double solarConstant, Double deltaT);
}
