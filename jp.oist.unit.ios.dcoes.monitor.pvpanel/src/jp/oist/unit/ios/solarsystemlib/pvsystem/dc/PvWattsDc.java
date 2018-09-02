package jp.oist.unit.ios.solarsystemlib.pvsystem.dc;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.pvsystem.cell.temperature.CellTemperature;

public class PvWattsDc {

    private ModelCollection models;

    public PvWattsDc(ModelCollection models) {
        this.models = models;
    }

    /**
     * Implements PVWatts DC power model [1]
     * reference temperature is set 25.0 degC
     * @param effectiveIrradiance Irradiance transmitted to the PV cells in W/m^2
     * @param temps Peripheral temperature of PV module
     * @param pdc0 Nameplate DC Power
     * @param gammaPdc Temperature coefficient for PV
     * @return DC power
     */
    public double estimate(double effectiveIrradiance, CellTemperature.Variable temps,
                           double pdc0, double gammaPdc) {
        return estimate(effectiveIrradiance, temps, pdc0, gammaPdc, 25.0);
    }

    /**
     * Implements PVWatts DC power model [1]
     * @param effectiveIrradiance Irradiance transmitted to the PV cells in W/m^2
     * @param temps Peripheral temperature of PV module
     * @param pdc0 Nameplate DC Power
     * @param gammaPdc Temperature coefficient for PV
     * @param refTemp Reference temperature for PV
     * @return DC power
     */
    public double estimate(double effectiveIrradiance, CellTemperature.Variable temps,
                           double pdc0, double gammaPdc, double refTemp) {
        return effectiveIrradiance * 0.001 * pdc0 * (1 + gammaPdc * (temps.cell - refTemp));
    }
}
