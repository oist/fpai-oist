/* vim: set ts=4 sw=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib;

import jp.oist.unit.ios.solarsystemlib.irradiance.Irradiance;

public class ClearSky {

    /**
     *
     * @param apparent_elevation
     * @param pressure air-pressure in pascal (Pa)
     * @param dni_extra
     * @return
     */
    public static Irradiance.Variable simplified_solis(double apparent_elevation, double pressure, double dni_extra) {
        return simplified_solis(apparent_elevation , 0.1 , 1 , pressure , dni_extra);
    }

    /**
     *
     * @param apparent_elevation
     * @param aod700
     * @param precipitable_water
     * @param pressure air-pressure in pascal (Pa)
     * @param dni_extra
     * @return
     */
    public static Irradiance.Variable simplified_solis(double apparent_elevation, double aod700,
                                                       double precipitable_water, double pressure,
                                                       double dni_extra) {
        double p = pressure;
        double w = (precipitable_water <0.2) ? 0.2 : precipitable_water ;

        //# algorithm fails for pw < 0.2
        //        # this algorithm is reasonably fast already, but it could be made
        //        # faster by precalculating the powers of aod700, the log(p/p0), and
        //        # the log(w) instead of repeating the calculations as needed in each
        //        # function
        double i0p = _calc_i0p(dni_extra, w, aod700, p);//

        double taub = _calc_taub(w, aod700, p);
        double b = _calc_b(w, aod700);

        double taug = _calc_taug(w, aod700, p);
        double g = _calc_g(w, aod700);

        double taud = _calc_taud(w, aod700, p);
        double d = _calc_d(aod700, p);//

        //# this prevents the creation of nans at night instead of 0s
        //# it's also friendly to scalar and series inputs

        double sin_elev = Math.sin(Math.toRadians(apparent_elevation));
        sin_elev = ( sin_elev < 1.e-30 ) ? 1.e-30 : sin_elev ;

        double dni = i0p * Math.exp(-taub/ Math.pow( sin_elev , b));
        double ghi = i0p * Math.exp(-taug/ Math.pow( sin_elev , g)) * sin_elev;
        double dhi = i0p * Math.exp(-taud/ Math.pow( sin_elev , d));

        return new Irradiance.Variable(ghi, dni, dni_extra, dhi, null);
    }

    private static double _calc_i0p(double i0, double w, double aod700, double p) {
        //"""Calculate the "enhanced extraterrestrial irradiance"."""
        double p0 = 101325.;
        double io0 = 1.08 * Math.pow( w , 0.0051);
        double i01 = 0.97 * Math.pow(w , 0.032);
        double i02 = 0.12 * Math.pow( w , 0.56);
        double i0p = i0 * ( i02* Math.pow( aod700 , 2 ) + i01*aod700 + io0 + 0.071* Math.log(p/p0));

        return i0p;
    }

    private static double _calc_taub(double w,double aod700,double p) {
        //"""Calculate the taub coefficient"""
        double p0 = 101325.;
        double tb1 = 1.82 + 0.056 * Math.log(w) + 0.0071 * Math.pow( Math.log(w) , 2);
        double tb0 = 0.33 + 0.045 * Math.log(w) + 0.0096 * Math.pow( Math.log(w) , 2);
        double tbp = 0.0089 * w + 0.13;

        double taub = tb1 * aod700 + tb0 + tbp * Math.log(p/p0);

        return taub;

    }

    private static double _calc_b(double w, double aod700) {
        // Calculate the b coefficient.
        double b1 = 0.00925 * Math.pow( aod700 ,2) + 0.0148*aod700 - 0.0172;
        double b0 = -0.7565 * Math.pow( aod700 , 2) + 0.5057*aod700 + 0.4557;

        double b = b1 * Math.log(w) + b0;

        return b;
    }

    private static double _calc_taug(double w, double aod700,double p) {
        // Calculate the taug coefficient.
        double p0 = 101325.;
        double tg1 = 1.24 + 0.047*Math.log(w) + 0.0061* Math.pow( Math.log(w) ,2 );
        double tg0 = 0.27 + 0.043*Math.log(w) + 0.0090* Math.pow( Math.log(w) ,2);
        double tgp = 0.0079*w + 0.1;
        double taug = tg1*aod700 + tg0 + tgp* Math.log(p/p0);

        return taug;
    }

    private static double _calc_g(double w, double aod700) {
        // Calculate the g coefficient.
        return -0.0147*Math.log(w) - 0.3079* Math.pow( aod700 , 2 ) + 0.2846*aod700 + 0.3798;
    }

    private static double _calc_d(double aod700, double p) {
        // Calculate the d coefficient.
        double p0 = 101325.;
        double dp = 1/(18 + 152*aod700);
        return -0.337* Math.pow(aod700 ,2 ) + 0.63*aod700 + 0.116 + dp* Math.log(p/p0);
    }

    public static double _calc_taud(double w, double aod700, double p) {
        double td4 = 0;
        double td3 = 0;
        double td2 = 0;
        double td1 = 0;
        double td0 = 0;
        double tdp = 0;
        if(aod700 < 0.05) {
            td4 = 86*w - 13800;
            td3 = -3.11*w + 79.4;
            td2 = -0.23*w + 74.8;
            td1 = 0.092*w - 8.86;
            td0 = 0.0042*w + 3.12;
            tdp = -0.83* Math.pow( (1 + aod700) , (-17.2));
        } else {
            td4 = -0.21*w + 11.6;
            td3 =  0.27*w - 20.7;
            td2 = -0.134*w + 15.5;
            td1 =  0.0554*w - 5.71;
            td0 =  0.0057*w + 2.94;
            tdp = -0.71*Math.pow((1+aod700) , (-15.0));
        };

        double p0 = 101325.;
        return td0
             + td1*aod700
             + td2 * Math.pow(aod700, 2)
             + td3 * Math.pow(aod700, 3)
             + td4 * Math.pow( aod700 , 4 )
             + tdp * Math.log(p/p0);
    }
}
