/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.atomsphere.airmass;

import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

/**
 * @author Ken Kuwae
 */
public class RelativeAirmassKastenYoung1989Model extends RelativeAirmassModel {

    private Double relative;

    /* coefficients */
    private static final double A = 0.50572;
    private static final double B = 6.07995;
    private static final double C = 1.6364;
    public RelativeAirmassKastenYoung1989Model(ModelCollection factory) {
        super(factory);
    }

    /**
     * estimate relative airmass by F Kasten, AT Young model
     * @param sp solar position
     * @return relative airmass
     * @see <a href="https://www.osapublishing.org/viewmedia.cfm?uri=ao-28-22-4735">Revised optical air mass tables and approximation formula</a>
     */
    @Override
    public Double estimate(SolarPosition.Variable sp) {
        if (sp.getApparentZenith() > 90.0)
            return null;
        double elev = 90 - sp.getApparentZenith();
        return 1.0/(Math.sin(Math.toRadians(elev)) + A * (Math.pow(B+(elev), -C)));
    }
}
