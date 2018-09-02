/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance.dni;

import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;

import java.time.ZonedDateTime;

public class DniErbsModel extends DniModel {

    public DniErbsModel(ModelCollection models) { super(models); }

    /**
     * Estimates DNI (Direct Normal Irradiance) value and some peripheral parameters.
     * @param ts target time
     * @param ghi Global horizontal irradiance (w/m^2)
     * @param zenith Solar zenith angle
     * @param vars thus aren't used.
     * @see DniModel
     */
    @Override
    public Irradiance.Variable estimate(ZonedDateTime ts, double ghi, double zenith, Object... vars) {
        Irradiance irradiance = factory.irradiance();
        double dniExtra = irradiance.extraRadiation(ts);
        double i0H = dniExtra * Math.cos(Math.toRadians(zenith));
        double kt = Math.max(ghi/i0H, 0.0);

        double df = 1 - 0.09*kt;
        if ((kt > 0.22) && (kt <= 0.8)) {
            df = 0.9511
               - 0.1604 * kt
               + 4.388  * Math.pow(kt, 2)
               - 16.638 * Math.pow(kt, 3)
               + 12.336 * Math.pow(kt, 4);
        } else if ((kt > 0.8)) {
            df = 0.165;
        }

        double dhi = df * ghi;
        double dni = (ghi - dhi) / Math.cos(Math.toRadians(zenith));

        return new Irradiance.Variable(ghi, dni, dniExtra, dhi, kt);
    }
}
