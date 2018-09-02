package jp.oist.unit.ios.solarsystemlib.pvsystem.ac;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public class PvWattsAc {

    private ModelCollection models;

    public PvWattsAc(ModelCollection models) {
        this.models = models;
    }

    /**
     * Implements PVWatts inverter model [1]
     * @param pdc DC Power
     * @param pdc0 Nameplate DC Power
     * @return estimated AC power
     */
    public double estimate(double pdc, double pdc0) {
        return estimate(pdc, pdc0, null, null);
    }

    /**
     * Implements PVWatts inverter model [1]
     * @param pdc DC Power
     * @param pdc0 Nameplate DC Power
     * @param nominalEfficiency Nominal inverter efficiency
     * @param referenceEfficiency Reference inverter efficiency
     * @return estimated AC power
     */
    public double estimate(double pdc, double pdc0, Double nominalEfficiency, Double referenceEfficiency) {
        nominalEfficiency = (nominalEfficiency == null)? 0.96 : nominalEfficiency;
        referenceEfficiency = (referenceEfficiency == null)? 0.9637 : referenceEfficiency;

        double pac0 = nominalEfficiency * pdc0;
        double zeta = pdc / pdc0;

        double eta = nominalEfficiency / referenceEfficiency * (-0.0162 * zeta - 0.0059/zeta + 0.9858);

        double pac = eta * pdc;
        return Math.max(pac, 0.0);
    }
}
