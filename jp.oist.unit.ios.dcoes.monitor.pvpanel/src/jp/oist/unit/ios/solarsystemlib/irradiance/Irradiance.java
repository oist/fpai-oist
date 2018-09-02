/* vim: set sw=4 ts=4 et fenc=utf-8 ff=unix cc=100 : */
package jp.oist.unit.ios.solarsystemlib.irradiance;

import jp.oist.unit.ios.solarsystemlib.solarposition.SolarPosition;
import jp.oist.unit.ios.solarsystemlib.atomsphere.airmass.Airmass;
import jp.oist.unit.ios.solarsystemlib.common.Consts;
import jp.oist.unit.ios.solarsystemlib.collection.ModelCollection;
import jp.oist.unit.ios.solarsystemlib.irradiance.dni.DniModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.extraradiation.ExtraRadiationModel;
import jp.oist.unit.ios.solarsystemlib.irradiance.skydiffuse.SkyDiffuseModel;

import java.time.ZonedDateTime;

public class Irradiance {

    public static enum SurfaceType {
        URBAN(0.18),
        GRASS(0.20),
        FRESHG_RASS(0.26),
        SOIL(0.17),
        SAND(0.40),
        SNOW(0.65),
        FRESH_SNOW(0.75),
        ASPHALT(0.12),
        CONCRETE(0.30),
        ALUMINUM(0.85),
        COPPER(0.74),
        FRESH_STEEL(0.35),
        DIRTY_STEEL(0.08);

        private final double albedo;

        private SurfaceType(final double albedo) { this.albedo = albedo; }

        public double getAlbedo() { return albedo; }
    }

    public static class PoaVariable {
        public final Double global, direct, diffuse, skyDiffuse, groundDiffuse;

        public PoaVariable (Double global, Double direct, Double diffuse, Double skyDiffuse, Double groundDiffuse) {
            this.global = global;
            this.direct = direct;
            this.diffuse = diffuse;
            this.skyDiffuse = skyDiffuse;
            this.groundDiffuse = groundDiffuse;
        }
    }

    public static class Variable {
        public final Double ghi, dni, dniExtra, dhi, kt;

        public Variable(Double ghi, Double dni, Double dniExtra, Double dhi, Double kt) {
            this.ghi = ghi;
            this.dni = dni;
            this.dniExtra= dniExtra;
            this.dhi = dhi;
            this.kt = kt;
        }

        @Override
        public String toString() {
            return String.format("{\n"
                        + "\t ghi      : %.6f,\n"
                        + "\t dni      : %.6f,\n"
                        + "\t dniExtra : %.6f,\n"
                        + "\t dhi      : %.6f,\n"
                        + "\t kt       : %.6f,\n"
                        + "}",
                        ghi, dni, dniExtra, dhi, kt);
        }
    }

    private ModelCollection factory;

    public Irradiance(ModelCollection factory)  {
        this.factory = factory;
    }

    public double extraRadiation(ZonedDateTime dt) {
        return extraRadiation(dt.getDayOfYear());
    }

    public double extraRadiation(int  doy) {
        return extraRadiation(doy, Consts.SOLAR_CONSTANT);
    }

    public double extraRadiation(ZonedDateTime dt, Double solarConstant) {
        return extraRadiation(dt.getDayOfYear(), solarConstant);
    }

    public double extraRadiation(int doy, Double solarConstant) {
        return extraRadiation(doy, solarConstant, 67.0);
    }

    public double extraRadiation(ZonedDateTime dt, Double solarConstant, Double deltaT) {
        return extraRadiation(dt.getDayOfYear(), solarConstant, deltaT);
    }

    public double extraRadiation(int doy, Double solarConstant, Double deltaT) {
        ExtraRadiationModel model = factory.getExtraRadiationModel();
        return model.estimate(doy, solarConstant, deltaT);
    }

    public static double projection(double surfaceTilt, double surfaceAzimuth,
                                    double solarZenith, double solarAzimuth) {
         return Math.cos(Math.toRadians(surfaceTilt)) * Math.cos(Math.toRadians(solarZenith))
                + Math.sin(Math.toRadians(surfaceTilt)) * Math.sin(Math.toRadians(solarZenith))
                * Math.cos(Math.toRadians(solarAzimuth-surfaceAzimuth));
    }

    public static double aoi(double surfaceTilt, double surfaceAzimuth,
                             double solarZenith, double solarAzimuth) {
        double projection = projection(surfaceTilt, surfaceAzimuth, solarZenith, solarAzimuth);
        return Math.toDegrees(Math.acos(projection));
    }

    public static double beamComponent(double surfaceTilt, double surfaceAzimuth,
                                       SolarPosition.Variable sp, Irradiance.Variable irrad) {
        return beamComponent(surfaceTilt, surfaceAzimuth, sp, irrad.dni);
    }

    public static double beamComponent(double surfaceTilt, double surfaceAzimuth,
                                       SolarPosition.Variable sp, double dni) {
        double beam = dni * projection(surfaceTilt, surfaceAzimuth, sp.getApparentZenith(), sp.getAzimuth());
        return Math.max(beam, 0.0);
    }

    public Variable getIrradiance(SolarPosition.Variable sp, double ghi, Object... vars) {
        DniModel model = factory.getDniModel();
        return model.estimate(sp.getDateTime(), ghi, sp.getApparentZenith(), vars);
    }

    public PoaVariable getPoaIrradiance(double surfaceTilt, double surfaceAzimuth,
                                        SolarPosition.Variable sp, Irradiance.Variable irrad,
                                        Airmass airmass, SurfaceType surfaceType, Object... vars) {
        double albedo = (surfaceType == null)? 0.25 : surfaceType.albedo;
        return getPoaIrradiance(surfaceTilt, surfaceAzimuth, sp, irrad, airmass, albedo);
    }

    public PoaVariable getPoaIrradiance(double surfaceTilt, double surfaceAzimuth,
                                     SolarPosition.Variable sp, Irradiance.Variable irrad,
                                     Airmass airmass, Double albedo, Object... vars) {
        double beam = beamComponent(surfaceTilt, surfaceAzimuth, sp, irrad);

        SkyDiffuseModel skyDiffuseModel = factory.getSkyDiffuseModel();
        double skyDiffuse = skyDiffuseModel.estimate(surfaceTilt, surfaceAzimuth, sp, irrad, airmass, vars);
        double groundDiffuse = irrad.ghi * albedo * ( 1- Math.cos(Math.toRadians(surfaceTilt))) * 0.5;

        double diffuse = skyDiffuse + groundDiffuse;

        double total = beam + diffuse;

        return new PoaVariable(total, beam, diffuse, skyDiffuse, groundDiffuse);
    }
}