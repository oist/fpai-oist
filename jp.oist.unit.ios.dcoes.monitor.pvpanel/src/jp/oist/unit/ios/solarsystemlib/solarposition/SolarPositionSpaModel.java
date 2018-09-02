/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.solarposition;

import java.time.ZonedDateTime;

import jp.oist.unit.ios.solarsystemlib.Location;
import jp.oist.unit.ios.solarsystemlib.atomsphere.Atmosphere;
import jp.oist.unit.ios.solarsystemlib.common.Consts;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;

public class SolarPositionSpaModel extends SolarPositionModel {

    public SolarPositionSpaModel(ModelCollection factory) {
        super(factory);
    }

    /**
     * Estimates solar postion
     * @param dt target date-time
     * @param location location of target place
     * @param pressure  air-pressure on Pascal
     * @param tempAir air-temperature on degC
     * @param vars extra variable for model calculation (Double atmosRefract, Double deltaT)
     * @return variable instance for solar position
     */
    @Override
    public SolarPosition.Variable estimate(ZonedDateTime dt, Location location,
                                           Double pressure, Double tempAir, Object... vars) {
        Double elev = location.altitude;
        if (tempAir == null)
            tempAir = Consts.DEFAULT_TEMPERATURE;
        if (location.altitude == null && pressure == null) {
            elev = 0.0;
            pressure = Consts.DEFAULT_PRESSURE;
        } else if (elev == null) {
            elev = Atmosphere.pres2alt(pressure);
        } else if (pressure == null) {
            pressure = Atmosphere.alt2pres(elev);
        }

        Double atmosRefract = 0.5667;
        Double deltaT = calculateDeltaT(dt.getYear(), dt.getMonthValue());

        if (vars.length > 0 && vars[0] != null)
            atmosRefract = (Double)vars[0];

        if (vars.length > 1 && vars[1] != null)
            deltaT = (Double)vars[1];

        return estimate(dt, location.latitude, location.longitude, elev, pressure, tempAir, atmosRefract, deltaT);
    }

    public SolarPosition.Variable estimate(ZonedDateTime dt, double lat, double lng, double elev,
                                           double pressure, double tempAir, double atmosRefract, double deltaT) {
        pressure = pressure * 0.01;

        double unixtime = (double)dt.toEpochSecond();
        double jd = julian_day(unixtime);
        double jde = julian_ephemeris_day(jd, deltaT);
        double jc = julian_century(jd);
        double jce = julian_ephemeris_century(jde);
        double jme = julian_ephemeris_millennium(jce);
        double r = heliocentric_radius_vector(jme);

        double l = heliocentric_longitude(jme);
        double b = heliocentric_latitude(jme);

        double Theta = geocentric_longitude(l);
        double beta = geocentric_latitude(b);
        double x0 = mean_elongation(jce);
        double x1 = mean_anomaly_sun(jce);
        double x2 = mean_anomaly_moon(jce);
        double x3 = moon_argument_latitude(jce);
        double x4 = moon_ascending_longitude(jce);

        double deltaPsi = longitude_nutation(jce, x0, x1, x2, x3, x4);
        double deltaEpsilon = obliquity_nutation(jce, x0, x1, x2, x3, x4);
        double epsilon0 = mean_ecliptic_obliquity(jme);
        double epsilon = true_ecliptic_obliquity(epsilon0, deltaEpsilon);
        double deltaTau = aberration_correction(r);
        double lamd = apparent_sun_longitude(Theta, deltaPsi, deltaTau);
        double v0 = mean_sidereal_time(jd, jc);
        double v = apparent_sidereal_time(v0, deltaPsi, epsilon);
        double alpha = geocentric_sun_right_ascension(lamd, epsilon, beta);
        double delta = geocentric_sun_declination(lamd, epsilon, beta);

        double m = sun_mean_longitude(jme);
        // equation of time
        double eot = equation_of_time(m, alpha, deltaPsi, epsilon);
        double h = local_hour_angle(v, lng, alpha);
        double xi = equatorial_horizontal_parallax(r);

        double u = uterm(lat);
        double x = xterm(u, lat, elev);
        double y = yterm(u, lat, elev);
        double deltaAlpha = parallax_sun_right_ascension(x, xi, h, delta);
        double deltaPrime = topocentric_sun_declination(delta, x, y, xi, deltaAlpha, h);

        double hPrime = topocentric_local_hour_angle(h, deltaAlpha);
        // elevation
        double e0 = topocentric_elevation_angle_without_atmosphere(lat, deltaPrime, hPrime);
        double delta_e = atmospheric_refraction_correction(pressure, tempAir, e0, atmosRefract);
        // apparent elevation
        double e = topocentric_elevation_angle(e0, delta_e);
        // apparent zenith
        double theta = topocentric_zenith_angle(e);
        // zenith
        double theta0 = topocentric_zenith_angle(e0);
        double gamma = topocentric_astronomers_azimuth(hPrime, deltaPrime, lat);
        // azimuth
        double phi = topocentric_azimuth_angle(gamma);

        return new SolarPosition.Variable(dt, e, theta, phi, e0, eot, theta0);
    }

    /**
     * from spa.calculate_deltat
     * @param year
     * @param month  1-12
     * @return
     */
    private double calculateDeltaT(int year, int month) {
        //if(year > 3000 || year < -1999) {
        //    return 0;
        //}

        double y = year + (month - 0.5) / 12;
        double deltat = 0;

        if (year < -500) {

            deltat = -20 + 32 * Math.pow(((y - 1820) / 100), 2);

        } else if (year < 500) {
            deltat = 10583.6
                    - 1014.41 * (y / 100)
                    + 33.78311 * Math.pow((y / 100), 2)
                    - 5.952053 * Math.pow((y / 100), 3)
                    - 0.1798452 * Math.pow((y / 100), 4)
                    + 0.022174192 * Math.pow((y / 100), 5)
                    + 0.0090316521 * Math.pow((y / 100), 6);

        } else if (year < 1600) {
            deltat = 1574.2
                    - 556.01 * ((y - 1000) / 100)
                    + 71.23472 * Math.pow(((y - 1000) / 100), 2)
                    + 0.319781 * Math.pow(((y - 1000) / 100), 3)
                    - 0.8503463 * Math.pow(((y - 1000) / 100), 4)
                    - 0.005050998 * Math.pow(((y - 1000) / 100), 5)
                    + 0.0083572073 * Math.pow(((y - 1000) / 100), 6);

        } else if (year < 1700) {
            deltat = 120
                    - 0.9808 * (y - 1600)
                    - 0.01532 * Math.pow((y - 1600), 2)
                    + Math.pow((y - 1600), 3) / 7129;

        } else if (year < 1800) {
            deltat = 8.83
                    + 0.1603 * (y - 1700)
                    - 0.0059285 * Math.pow((y - 1700), 2)
                    + 0.00013336 * Math.pow((y - 1700), 3)
                    - Math.pow((y - 1700), 4) / 1174000;

        } else if (year < 1860) {
            deltat = 13.72
                    - 0.332447 * (y - 1800)
                    + 0.0068612 * Math.pow((y - 1800), 2)
                    + 0.0041116 * Math.pow((y - 1800), 3)
                    - 0.00037436 * Math.pow((y - 1800), 4)
                    + 0.0000121272 * Math.pow((y - 1800), 5)
                    - 0.0000001699 * Math.pow((y - 1800), 6)
                    + 0.000000000875 * Math.pow((y - 1800), 7);

        } else if (year < 1900) {
            deltat = 7.6
                    + 0.5737 * (y - 1860)
                    - 0.251754 * Math.pow((y - 1860), 2)
                    + 0.01680668 * Math.pow((y - 1860), 3)
                    - 0.0004473624 * Math.pow((y - 1860), 4)
                    + Math.pow((y - 1860), 5) / 233174;

        } else if (year < 1920) {
            deltat = -2.79
                    + 1.494119 * (y - 1900)
                    - 0.0598939 * Math.pow((y - 1900), 2)
                    + 0.0061966 * Math.pow((y - 1900), 3)
                    - 0.000197 * Math.pow((y - 1900), 4);

        } else if (year < 1941) {
            deltat = 21.20
                    + 0.84493 * (y - 1920)
                    - 0.076100 * Math.pow((y - 1920), 2)
                    + 0.0020936 * Math.pow((y - 1920), 3);

        } else if (year < 1961) {
            deltat = 29.07
                    + 0.407 * (y - 1950)
                    - Math.pow((y - 1950), 2) / 233
                    + Math.pow((y - 1950), 3) / 2547;

        } else if (year < 1986) {
            deltat = 45.45
                    + 1.067 * (y - 1975)
                    - Math.pow((y - 1975), 2) / 260
                    - Math.pow((y - 1975), 3) / 718;

        } else if (year < 2005) {
            deltat = 63.86
                    + 0.3345 * (y - 2000)
                    - 0.060374 * Math.pow((y - 2000), 2)
                    + 0.0017275 * Math.pow((y - 2000), 3)
                    + 0.000651814 * Math.pow((y - 2000), 4)
                    + 0.00002373599 * Math.pow((y - 2000), 5);

        } else if (year < 2050) {
            deltat = 62.92
                    + 0.32217 * (y - 2000)
                    + 0.005589 * Math.pow((y - 2000), 2);

        } else if (year < 2150) {
            deltat = -20
                    + 32 * Math.pow(((y - 1820) / 100), 2)
                    - 0.5628 * (2150 - y);

        } else {
            deltat = -20
                    + 32 * Math.pow(((y - 1820) / 100), 2);
        }

        return deltat;

    }

    private double topocentric_azimuth_angle(double topocentric_astronomers_azimuth) {
        double phi = topocentric_astronomers_azimuth + 180;
        return phi % 360;
    }

    private double topocentric_astronomers_azimuth(double topocentric_local_hour_angle,
                                                   double topocentric_sun_declination,
                                                   double observer_latitude) {

        double num = Math.sin(Math.toRadians(topocentric_local_hour_angle));
        double denom = (Math.cos(Math.toRadians(topocentric_local_hour_angle))
                         * Math.sin(Math.toRadians(observer_latitude))
                         - Math.tan(Math.toRadians(topocentric_sun_declination))
                         * Math.cos(Math.toRadians(observer_latitude)));
        double gamma = Math.toDegrees(Math.atan2(num, denom));
        return gamma % 360;

    }

    private double topocentric_zenith_angle(double topocentric_elevation_angle) {
        double theta = 90 - topocentric_elevation_angle;
        return theta;

    }

    private double topocentric_elevation_angle(double topocentric_elevation_angle_without_atmosphere, double atmospheric_refraction_correction) {
        double e = (topocentric_elevation_angle_without_atmosphere
                + atmospheric_refraction_correction);
        return e;

    }

    private double atmospheric_refraction_correction(double local_pressure, double local_temp,
            double topocentric_elevation_angle_wo_atmosphere, double atmos_refract) {

        boolean bSwitch = ( topocentric_elevation_angle_wo_atmosphere >= ( -1.0 * ( 0.26667 + atmos_refract)) );
        double dSwitch = bSwitch ? 1: 0;

        double delta_e = ((local_pressure / 1010.0) * (283.0 / (273 + local_temp))
                       * 1.02 / (60 * Math.tan(Math.toRadians(
                           topocentric_elevation_angle_wo_atmosphere
                           + 10.3 / (topocentric_elevation_angle_wo_atmosphere
                                     + 5.11))))) * dSwitch;

        return delta_e;

    }

    private double topocentric_elevation_angle_without_atmosphere(double observer_latitude,
            double topocentric_sun_declination,  double topocentric_local_hour_angle) {

        double e0 = Math.toDegrees(Math.asin(
                Math.sin(Math.toRadians(observer_latitude))
                * Math.sin(Math.toRadians(topocentric_sun_declination))
                + Math.cos(Math.toRadians(observer_latitude))
                * Math.cos(Math.toRadians(topocentric_sun_declination))
                * Math.cos(Math.toRadians(topocentric_local_hour_angle))));
        return e0;

    }

    private double topocentric_local_hour_angle(double local_hour_angle, double parallax_sun_right_ascension) {
        double H_prime = local_hour_angle - parallax_sun_right_ascension;
        return H_prime;
    }

    private double topocentric_sun_declination(double geocentric_sun_declination, double xterm, double yterm,
            double equatorial_horizontal_parallax,
            double parallax_sun_right_ascension,
            double local_hour_angle) {

        double num = ((Math.sin(Math.toRadians(geocentric_sun_declination)) - yterm
                * Math.sin(Math.toRadians(equatorial_horizontal_parallax)))
               * Math.cos(Math.toRadians(parallax_sun_right_ascension)));
        double denom = (Math.cos(Math.toRadians(geocentric_sun_declination)) - xterm
                 * Math.sin(Math.toRadians(equatorial_horizontal_parallax))
                 * Math.cos(Math.toRadians(local_hour_angle)));
        double delta = Math.toDegrees(Math.atan2(num, denom));
        return delta;

    }

    private double parallax_sun_right_ascension(double xterm, double equatorial_horizontal_parallax,
            double local_hour_angle, double geocentric_sun_declination) {

        double num = (-xterm * Math.sin(Math.toRadians(equatorial_horizontal_parallax))
                * Math.sin(Math.toRadians(local_hour_angle)));
        double denom = (Math.cos(Math.toRadians(geocentric_sun_declination))
                  - xterm * Math.sin(Math.toRadians(equatorial_horizontal_parallax))
                  * Math.cos(Math.toRadians(local_hour_angle)));
        double delta_alpha = Math.toDegrees(Math.atan2(num, denom));
        return delta_alpha;

    }

    private double yterm(double u, double observer_latitude, double observer_elevation) {
        double y = (0.99664719 * Math.sin(u) + observer_elevation / 6378140
                * Math.sin(Math.toRadians(observer_latitude)));
        return y;

    }

    private double xterm(double u, double observer_latitude, double observer_elevation) {
        double x = (Math.cos(u) + observer_elevation / 6378140
                        * Math.cos(Math.toRadians(observer_latitude)));
        return x;

    }

    private double uterm(double observer_latitude) {
        double u = Math.atan(0.99664719 * Math.tan(Math.toRadians(observer_latitude)));
        return u;
    }

    private double equatorial_horizontal_parallax(double earth_radius_vector) {
        double xi = 8.794 / (3600 * earth_radius_vector);
        return xi;
    }

    private double local_hour_angle(double apparent_sidereal_time, double observer_longitude, double sun_right_ascension) {
        double H = apparent_sidereal_time + observer_longitude - sun_right_ascension;
        return H % 360.0d;
    }

    private double equation_of_time(double sun_mean_longitude, double geocentric_sun_right_ascension,
            double longitude_nutation, double true_ecliptic_obliquity) {
        double E = (sun_mean_longitude - 0.0057183 - geocentric_sun_right_ascension +
                longitude_nutation * Math.cos(Math.toRadians(true_ecliptic_obliquity)) );
        // limit between 0 and 360
        E = E % 360;
        // convert to minutes
        E *= 4;
        E = (E > 20) ? (E - 1440): ((E < -20)? (E + 1440) : E);
        return E;

    }

    private double sun_mean_longitude(double julian_ephemeris_millennium) {
        double M = (280.4664567 + 360007.6982779 * julian_ephemeris_millennium
                + 0.03032028 * Math.pow( julian_ephemeris_millennium , 2)
                + Math.pow( julian_ephemeris_millennium , 3) / 49931.0
                - Math.pow( julian_ephemeris_millennium , 4) / 15300.0
                - Math.pow( julian_ephemeris_millennium , 5) / 2e6);
           return M;
    }

    private double geocentric_sun_declination(double apparent_sun_longitude, double true_ecliptic_obliquity, double geocentric_latitude) {
        double delta = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(geocentric_latitude)) *
                                                   Math.cos(Math.toRadians(true_ecliptic_obliquity)) +
                                                   Math.cos(Math.toRadians(geocentric_latitude)) *
                                                   Math.sin(Math.toRadians(true_ecliptic_obliquity)) *
                                                   Math.sin(Math.toRadians(apparent_sun_longitude))
                                                   ));

        return delta;

    }

    private double geocentric_sun_right_ascension(double apparent_sun_longitude, double true_ecliptic_obliquity, double geocentric_latitude) {
        double num = (Math.sin(Math.toRadians(apparent_sun_longitude))
                * Math.cos(Math.toRadians(true_ecliptic_obliquity))
                - Math.tan(Math.toRadians(geocentric_latitude))
                * Math.sin(Math.toRadians(true_ecliptic_obliquity)));

        double alpha = Math.toDegrees(Math.atan2(num, Math.cos(Math.toRadians(apparent_sun_longitude))));

        return alpha % 360;
    }

    private double apparent_sidereal_time(double mean_sidereal_time, double longitude_nutation, double true_ecliptic_obliquity) {
        double v = mean_sidereal_time + longitude_nutation * Math.cos(Math.toRadians(true_ecliptic_obliquity));

        return v;
    }

    private double mean_sidereal_time(double julian_day, double julian_century) {
        double v0 = (280.46061837 + 360.98564736629 * (julian_day - 2451545)
                + 0.000387933 * Math.pow(julian_century, 2) - Math.pow(julian_century, 3) / 38710000);

        return v0 % 360.0;
    }

    private double apparent_sun_longitude(double geocentric_longitude, double longitude_nutation, double aberration_correction) {
        return  geocentric_longitude + longitude_nutation + aberration_correction;
    }

    private double aberration_correction(double earth_radius_vector) {
        double deltau = -20.4898 / (3600 * earth_radius_vector);
        return deltau;
    }

    private double true_ecliptic_obliquity(double mean_ecliptic_obliquity , double obliquity_nutation) {
        double e0 = mean_ecliptic_obliquity;
        double deleps = obliquity_nutation;
        double e = e0*1.0/3600 + deleps;
        return e;
    }

    // mean brothers
    private double mean_ecliptic_obliquity(double julian_ephemeris_millennium) {
        double U = 1.0 * julian_ephemeris_millennium/10;
        double e0 = (84381.448
                - 4680.93 * U
                - 1.55    * Math.pow(U , 2)
                + 1999.25 * Math.pow(U , 3)
                - 51.38   * Math.pow(U , 4)
                - 249.67  * Math.pow(U , 5)
                - 39.05   * Math.pow(U , 6)
                + 7.12    * Math.pow(U , 7)
                + 27.87   * Math.pow(U , 8)
                + 5.79    * Math.pow(U , 9)
                + 2.45    * Math.pow(U , 10) );
        return e0;
    }

    private double mean_elongation(double julian_ephemeris_century) {
        double x0 = (297.85036
                + 445267.111480 * julian_ephemeris_century
                - 0.0019142 * Math.pow(julian_ephemeris_century , 2)
                + Math.pow(julian_ephemeris_century , 3) / 189474);
        return x0;
    }

    private double mean_anomaly_sun(double julian_ephemeris_century) {
        double x1 = (357.52772
                + 35999.050340 * julian_ephemeris_century
                - 0.0001603 * Math.pow( julian_ephemeris_century , 2)
                - Math.pow( julian_ephemeris_century ,3) / 3e5);
        return x1;
    }

    private double mean_anomaly_moon(double julian_ephemeris_century) {
        double x2 = (134.96298
                + 477198.867398 * julian_ephemeris_century
                + 0.0086972 * Math.pow(julian_ephemeris_century , 2)
                + Math.pow(julian_ephemeris_century , 3) / 56250);

        return x2;
    }

    private double moon_argument_latitude(double julian_ephemeris_century) {
        double x3 = (93.27191
                + 483202.017538 * julian_ephemeris_century
                - 0.0036825 * Math.pow( julian_ephemeris_century , 2)
                + Math.pow( julian_ephemeris_century , 3) / 327270);

        return x3;
    }

    private double moon_ascending_longitude(double julian_ephemeris_century) {
        double x4 = (125.04452
                - 1934.136261 * julian_ephemeris_century
                + 0.0020708 * Math.pow( julian_ephemeris_century , 2)
                + Math.pow( julian_ephemeris_century , 3) / 450000);

        return x4;
    }

    private double longitude_nutation(double julian_ephemeris_century , double x0 ,double x1 ,double x2 ,double x3 ,double x4 ) {
        double delta_psi_sum = 0;
        double[][] n_y_arr = Consts.NUTATION_YTERM_ARRAY;
        double[][] abcd_arr = Consts.NUTATION_ABCD_ARRAY;
        for(int i=0; i<n_y_arr.length; i++) {
            double a = abcd_arr[i][0];
            double b = abcd_arr[i][1];

            double argsin = n_y_arr[i][0] * x0
                          + n_y_arr[i][1] * x1
                          + n_y_arr[i][2] * x2
                          + n_y_arr[i][3] * x3
                          + n_y_arr[i][4] * x4;
            double term = (a + b * julian_ephemeris_century) * Math.sin(Math.toRadians(argsin));
            delta_psi_sum += term;
        }

        return delta_psi_sum / 36e6;
    }

    private double obliquity_nutation(double julian_ephemeris_century , double x0 ,double x1 ,double x2 ,double x3 ,double x4) {
        double delta_eps_sum = 0;
        double[][] n_y_arr = Consts.NUTATION_YTERM_ARRAY;
        double[][] abcd_arr = Consts.NUTATION_ABCD_ARRAY;
        for (int i=0; i<n_y_arr.length; i++) {
            double c = abcd_arr[i][2];
            double d = abcd_arr[i][3];

            double argcos = n_y_arr[i][0] * x0
                    + n_y_arr[i][1] * x1
                    + n_y_arr[i][2] * x2
                    + n_y_arr[i][3] * x3
                    + n_y_arr[i][4] * x4;
            double term = (c + d * julian_ephemeris_century) * Math.cos(Math.toRadians(argcos));
            delta_eps_sum += term;
        }

        return  delta_eps_sum / 36e6;
    }

    private double geocentric_longitude(double heliocentric_longitude) {
        double theta = heliocentric_longitude + 180.0;
        return theta % 360;
    }

    private double geocentric_latitude(double heliocentric_latitude) {
        return  -1.0 * heliocentric_latitude;
    }

    private double julian_day(double unixtime) {
        return unixtime / 86400.0d + 2440587.5d;
    }

    private double julian_ephemeris_day(double julian_day, double delta_t) {
        return julian_day + delta_t * 1.0 / 86400;
    }

    private double julian_century(double julian_day) {
        return (julian_day - 2451545.0d) / 36525.0d;
    }

    private double julian_ephemeris_century(double julian_ephemeris_day) {
        return (julian_ephemeris_day - 2451545)  / 36525.0d;
    }

    private double julian_ephemeris_millennium(double julian_ephemeris_century) {
        return julian_ephemeris_century / 10.0d;
    }

    private double heliocentric_radius_vector(double jme) {
        double r0 = 0, r1 = 0, r2 = 0, r3 = 0, r4 = 0;
        double[][][] hrt = Consts.HELIO_RADIUS_TABLE;

        for (int i=0; i<hrt[0].length; i++)
            r0 += hrt[0][i][0] * Math.cos(hrt[0][i][1] + hrt[0][i][2] * jme);

        for (int i=0; i<hrt[1].length; i++)
            r1 += hrt[1][i][0] * Math.cos(hrt[1][i][1] + hrt[1][i][2] * jme);

        for (int i=0; i<hrt[2].length; i++)
            r2 += hrt[2][i][0] * Math.cos(hrt[2][i][1] + hrt[2][i][2] * jme);

        for (int i=0; i<hrt[3].length; i++)
            r3 += hrt[3][i][0] * Math.cos(hrt[3][i][1] + hrt[3][i][2] * jme);

        for (int i=0; i<hrt[4].length; i++)
            r4 += hrt[4][i][0] * Math.cos(hrt[4][i][1]+hrt[4][i][2]*jme);

        return (r0 + r1 * jme + r2 * Math.pow(jme, 2) + r3 * Math.pow(jme, 3) + r4 * Math.pow(jme, 4)) / 1e8;
    }

    private double heliocentric_longitude(double jme) {
        double l0 = 0, l1 = 0, l2 = 0, l3 = 0, l4 = 0, l5 = 0;
        double[][][] hlt = Consts.HELIO_LONG_TABLE;

        for (int i=0; i<hlt[0].length; i++)
            l0 += hlt[0][i][0] * Math.cos(hlt[0][i][1] + hlt[0][i][2] * jme);

        for (int i=0; i<hlt[1].length; i++)
            l1 += hlt[1][i][0] * Math.cos(hlt[1][i][1] + hlt[1][i][2] * jme);

        for (int i=0; i<hlt[2].length; i++)
            l2 += hlt[2][i][0] * Math.cos(hlt[2][i][1] + hlt[2][i][2] * jme);

        for (int i=0; i<hlt[3].length; i++)
            l3 += hlt[3][i][0] * Math.cos(hlt[3][i][1] + hlt[3][i][2] * jme);

        for (int i=0; i<hlt[4].length; i++)
            l4 += hlt[4][i][0] * Math.cos(hlt[4][i][1]+hlt[4][i][2]*jme);

        for (int i=0; i<hlt[5].length; i++)
            l5 += hlt[5][i][0] * Math.cos(hlt[5][i][1]+hlt[5][i][2]*jme);

        double l_rad = (l0 + l1 * jme + l2 * Math.pow(jme, 2) + l3 * Math.pow(jme, 3) + l4 * Math.pow(jme, 4)
                + l5 * Math.pow(jme, 5)) / 1e8;
        double l = Math.toDegrees(l_rad);

        return l % 360;
    }

    private double heliocentric_latitude(double jme) {
        double b0 = 0, b1 = 0;
        double[][][] hlt = Consts.HELIO_LAT_TABLE;

        for (int i=0; i<hlt[0].length; i++)
            b0 += hlt[0][i][0] * Math.cos(hlt[0][i][1] + hlt[0][i][2] * jme);

        for (int i=0; i<hlt[1].length; i++)
            b1 += hlt[1][i][0] * Math.cos(hlt[1][i][1] + hlt[1][i][2] * jme);

        return Math.toDegrees((b0 + b1 * jme) / 1e8);
    }
}
