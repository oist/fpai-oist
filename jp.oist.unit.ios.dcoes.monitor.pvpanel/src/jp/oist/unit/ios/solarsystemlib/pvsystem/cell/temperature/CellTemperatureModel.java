/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;
import jp.oist.unit.ios.solarsystemlib.pvsystem.PvSystem;

public abstract class CellTemperatureModel {

    protected ModelCollection factory;

    public CellTemperatureModel(ModelCollection factory) {
        this.factory = factory;
    }

    public abstract CellTemperature.Variable estimate(Irradiance.PoaVariable poa, double tempAir, double windSpeed,
                                                      PvSystem.RackingModel rackingModel);
}
