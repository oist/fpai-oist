/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.extraradiation;

import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public class ExtraRadiationSpencerModel extends  ExtraRadiationModel {

    public ExtraRadiationSpencerModel(ModelCollection factory) {
        super(factory);
    }

    @Override
    public Double estimate(int doy, Double solarConstant, Double deltaT) {
        double b = SolarPosition.dayAngle(doy);
        double roverR0Sqrd = (1.00011 + 0.034221 * Math.cos(b) + 0.00128 * Math.sin(b) +
                0.000719 * Math.cos(2 * b) + 7.7e-05 * Math.sin(2 * b));

        return solarConstant * roverR0Sqrd;
    }
}
