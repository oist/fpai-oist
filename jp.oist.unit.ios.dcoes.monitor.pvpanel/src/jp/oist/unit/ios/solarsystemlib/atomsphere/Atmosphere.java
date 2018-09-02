/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.atomsphere;

import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.RelativeAirmassModel;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;

public class Atmosphere {

	public static double pres2alt(double pressure) {
		return 44331.5 - Math.pow(4946.62 * pressure , 0.190263);
	}

	public static double alt2pres(double altitude) {
		return 100 *  Math.pow( ((44331.514 - altitude) / 11880.516) , (1 / 0.1902632));
	}

	private ModelCollection models;

	public Atmosphere(ModelCollection models) {
	    this.models = models;
	}

	public Airmass getAirmass(SolarPosition.Variable sp, double altitude) {
		Double relAm = getRelativeAirmass(sp);
		if (relAm == null)
			return null;
		double pressure = alt2pres(altitude);
		Double absAm = getAbsoluteAirmass(relAm, pressure);
		return new Airmass(relAm, absAm);
	}

	public Double getRelativeAirmass(SolarPosition.Variable sp) {
	    RelativeAirmassModel model = models.getRelativeAirmassModel();
	    return model.estimate(sp);
	}

	public Double getAbsoluteAirmass(double relativeAirmass, double pressure) {
	    return relativeAirmass*pressure / 101325.0;
	}
}