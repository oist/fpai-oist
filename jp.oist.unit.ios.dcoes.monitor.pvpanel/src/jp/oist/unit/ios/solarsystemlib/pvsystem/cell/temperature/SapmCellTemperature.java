/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;
import jp.oist.unit.ios.solarsystemlib.pvsystem.PvSystem;

public class SapmCellTemperature extends CellTemperatureModel {

    public SapmCellTemperature(ModelCollection models) {
        super(models);
    }

    @Override
    public CellTemperature.Variable estimate(Irradiance.PoaVariable poa, double tempAir, double windSpeed,
                                             PvSystem.RackingModel rackingModel) {
        // reference irradiation
        double[] vars = getCellTemperatureVariables(rackingModel);
        assert vars != null : "Unkown racking model";
        if (vars == null)
            return null;

        double a = vars[0];
        double b = vars[1];
        double deltaT = vars[2];
        return estimate(poa, tempAir, windSpeed, a, b, deltaT);
    }

    public CellTemperature.Variable estimate(Irradiance.PoaVariable poa, double tempAir, double windSpeed,
                                             double a, double b, double deltaT) {
        double e0 = 1000.0;
        double tempModule = poa.global * Math.exp(a + b*windSpeed)+tempAir;
        double tempCell = tempModule + (poa.global / e0) * deltaT;

        return new CellTemperature.Variable(tempModule, tempCell);
    }

    public double[] getCellTemperatureVariables(PvSystem.RackingModel rackingModel) {
        switch(rackingModel) {
            case OPEN_RACK_CELL_GLASSBACK:
                return new double[]{-3.47, -0.0594, 3.0};
            case ROOF_MOUNT_CELL_GLASSBACK:
                return new double[]{-2.98, -0.0471, 1};
            case OPEN_RACK_CELL_POLYMERBACK:
                return new double[]{-3.56, -0.0750, 3};
            case INSULATED_RACK_CELL_POLYMERBACK:
                return new double[]{-2.81, -0.455, 0};
            case OPEN_RACK_POLYMER_THINFILM_STEEL:
                return new double[]{-3.58, -0.113, 3};
            case X22_CONCENTRATOR_TRACKER:
                return new double[]{-3.23, -0.130, 13};
        }
        return null;
    }
}
